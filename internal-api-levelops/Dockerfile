FROM levelops/java-base:latest
COPY build/libs/internal-api-0.0.1-SNAPSHOT.jar /usr/src/myapp/internal-api-0.0.1-SNAPSHOT.jar
WORKDIR /usr/src/myapp

ENV JAVA_OPTS="-Xms2G -Xmx2G"
CMD ["bash", "-c", "java $JAVA_OPTS -jar internal-api-0.0.1-SNAPSHOT.jar"]

