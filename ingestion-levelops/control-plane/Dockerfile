FROM openjdk:17
COPY build/libs/control-plane-0.0.1-SNAPSHOT.jar /usr/src/myapp/control-plane-0.0.1-SNAPSHOT.jar
WORKDIR /usr/src/myapp
ENV JAVA_OPTS=""
CMD ["bash", "-c", "java $JAVA_OPTS -jar control-plane-0.0.1-SNAPSHOT.jar"]