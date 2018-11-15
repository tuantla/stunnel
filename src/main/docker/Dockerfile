FROM jeanblanchard/java:jdk-8u92

ENV JVM_XMX 128m
ENV SSH_HOST SSH_HOST
ENV SSH_USER SSH_USER
ENV SSH_PORT SSH_PORT
ENV SSH_PASSWORD SSH_PASSWORD
ENV BINDING_ADDRESS 0.0.0.0
ENV FORWARDING 2222:127.0.0.1:22

WORKDIR /opt/app/

ENTRYPOINT ["bin/entrypoint.sh"]

COPY repo/ /opt/app/repo/
COPY bin/ /opt/app/bin/
RUN ln -s /opt/jdk/bin/java bin/stunnel-java
RUN chmod a+x bin/entrypoint.sh
COPY jars/ /opt/app/repo/
