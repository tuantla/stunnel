# Java ssh tunnel

## build with sbt
`sbt clean test dockerPackage`

## docker run 
```
docker run -e SSH_HOST=127.0.0.1 -e SSH_PORT=22 -eSSH_USER=root -e SSH_PASSWORD=<ROOT_PASSWORD> -e FORWARDING=2222:127.0.0.1:22  indexing/stunnel

```