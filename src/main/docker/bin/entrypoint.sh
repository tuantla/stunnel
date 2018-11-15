#!/bin/sh
exec bin/stunnel-java \
    -cp $(cat bin/classpath) \
    -XX:+UseG1GC \
    -Xms32m \
    -Xmx$JVM_XMX \
    -Dfile.encoding="UTF-8" \
    -Duser.language=en \
    -Duser.country=US \
    -Djava.io.tmpdir=/tmp \
    -XX:+PrintCommandLineFlags \
    -XX:+PrintGC \
    -XX:+PrintGCTimeStamps \
    -DsshHost=$SSH_HOST \
    -DsshPort=$SSH_PORT \
    -DsshUser=$SSH_USER \
    -DsshPassword=$SSH_PASSWORD \
    -DbindingAddress=$BINDING_ADDRESS \
    -Dforwards=$FORWARDING \
  robinvn.stunnel.STunnel
