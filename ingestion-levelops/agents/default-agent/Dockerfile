FROM openjdk:17
COPY build/libs/default-agent-0.0.1-SNAPSHOT.jar /usr/src/myapp/default-agent-0.0.1-SNAPSHOT.jar
WORKDIR /usr/src/myapp
ARG BUILD_VERSION

ENV BUILD_VERSION=${BUILD_VERSION:-Unknown}

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=75"

CMD ["bash", "-c", "java $JAVA_OPTS -jar default-agent-0.0.1-SNAPSHOT.jar"]
