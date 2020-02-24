FROM shipilev/openjdk-shenandoah:8-fastdebug
RUN set -x \
    && apt update \
	&& apt install -y zip

COPY target/shenandoah-akka-bug-1.0.0-dist.zip  /shenandoah-akka-bug-1.0.0-dist.zip
RUN unzip /shenandoah-akka-bug-1.0.0-dist.zip -d /opt/
RUN rm -f /shenandoah-akka-bug-1.0.0-dist.zip

WORKDIR /opt/shenandoah-akka-bug-1.0.0/bin/
ENTRYPOINT ["java", "-Xmx8M", "-XX:+UseShenandoahGC", "-jar", "shenandoah-akka-bug-1.0.0.jar"]
