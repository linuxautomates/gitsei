FROM openjdk:17
COPY build/libs/etl-worker-0.0.1-SNAPSHOT.jar /app/etl-worker-0.0.1-SNAPSHOT.jar
WORKDIR /app

ENV JAVA_OPTS="-Xms2G -Xmx2G"
CMD ["bash", "-c", "java $JAVA_OPTS -jar etl-worker-0.0.1-SNAPSHOT.jar"]
