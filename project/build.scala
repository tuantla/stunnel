import java.io.ByteArrayInputStream
import java.security.{DigestInputStream, MessageDigest}
import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets

import sbt.Keys._
import sbt.Keys.name
import sbt.Keys.streams
import sbt.Keys.target
import sbt._
import sbt.taskKey

import scala.util.Try
import scala.collection.JavaConverters._
import build.checkChanged
import spray.revolver.RevolverPlugin._
import spray.revolver.RevolverPlugin.Revolver._
import spray.revolver.Actions._
object build extends com.typesafe.sbt.pom.PomBuild {


  lazy val defaultSettings = Seq(
    cancelable in Global := true,
    crossPaths in ThisBuild := false,
    scalaVersion in ThisBuild := "2.12.2",
    publishArtifact in packageDoc := false,
    publishArtifact in packageSrc := false,
    javacOptions in ThisBuild ++= Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-encoding", "UTF-8"
    ),
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
      "Built-By" -> System.getenv("USER"),
      "Implementation-Version" -> java.time.Instant.now().toString
    ),    
    exportJars := true,    
    exportJars in Test := false,
    libraryDependencies ++= Seq("com.novocode" % "junit-interface" % "0.11" % "test"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-s"),
    fork in Test := true,
    parallelExecution in Test := false
  ) ++ Revolver.settings

  /**
    * Check if directory content has changed since last execution.
    * Used to only execute tasks incrementally on changes.
    */
  def checkChanged(timestampFile: File, glob: PathFinder, excludes: Seq[String] = Nil): Boolean = {
    try{
    val prevLastModified: Long = Try { IO.read(timestampFile).toLong } getOrElse 0L
    timestampFile.delete()

    glob.get.toVector
      .filter {f =>  f.isFile && !excludes.exists(f.getName.endsWith(_)) }
      .sortBy { _.lastModified() } match {
        case list if list.isEmpty => false
        case list                 =>
          val lastModifiedFile: File = list.last
           IO.write(timestampFile, lastModifiedFile.lastModified().toString)
           val changed = lastModifiedFile.lastModified() != prevLastModified
           if (changed) {
             val df = new java.text.SimpleDateFormat("dd-MM-yyyy HH-mm-ss").format(new java.util.Date(lastModifiedFile.lastModified))
             println(s"${lastModifiedFile.getCanonicalPath} changed at ${df}")
           }
           changed
      }
    } catch{
      case e:java.lang.Exception =>
        e.printStackTrace()
        false
    }
  }

  def computeHash(fileContent: String): String = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }

  def computeHash(file: File): String = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(file), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }

  lazy val dockerPackage = taskKey[Unit]("Package app jars and bin in docker target folder")
  lazy val dockerCompose = taskKey[Unit]("Builds a docker using docker-compose")

  lazy val defaultDockerCompose = Def.task {
    println(dockerPackage.value)
    val stageFolder: File = target.value
    if (!checkChanged(timestampFile = stageFolder / ".dockerlast", glob = stageFolder.***)) {
      streams.value.log.info("No changes in " + stageFolder)
    } else {
      s"docker-compose rm -f ${name.value}".!
      s"docker-compose build ${name.value}".!
      s"docker-compose up -d --no-deps ${name.value}".!
    }
  }

  lazy val dockerSettings = Seq(

    dockerPackage := {
      val dockerTarget: File = baseDirectory.value / "target" / "docker"
      val classPathB = Vector.newBuilder[String]
      var fileCount = 0
      mappings in (Compile, packageSrc) ++= {
        val base  = (sourceManaged  in Compile).value
        val files = (managedSources in Compile).value
        files.map { f => (f, f.relativeTo(base).get.getPath) }
      }

      for (jar: Attributed[File] <- (fullClasspath in Compile).value) {
        val m: ModuleID = jar.metadata(Keys.moduleID.key)
        val dir: String = if (m.revision.endsWith("-SNAPSHOT")) "jars" else "repo"
        val pathSegments: Array[String] = dir +: m.organization.split('.') :+ m.name :+ m.revision :+ jar.data.name
        val path: String = pathSegments.mkString("/")
        IO.copyFile(jar.data, new File(dockerTarget, path), preserveLastModified = true)
        fileCount += 1
        classPathB += "repo/"+pathSegments.drop(1).mkString("/")
      }
      println(s"Copied $fileCount jar-files to docker target")

      IO.copyDirectory(baseDirectory.value / "src" / "main" / "docker", dockerTarget)
      IO.write(dockerTarget / "bin" / "classpath", classPathB.result.mkString(":"))
    },

    dockerCompose := defaultDockerCompose.value
  )





  override def projectDefinitions(baseDirectory: File): Seq[Project] = {
    super.projectDefinitions(baseDirectory) map { project: Project =>
      var p = project
        .settings(spray.revolver.RevolverPlugin.Revolver.settings)
        .settings(defaultSettings)

      if (new File(project.base, "src/main/docker").exists())
        p = p.settings(dockerSettings)
      p
    }
  }
}
