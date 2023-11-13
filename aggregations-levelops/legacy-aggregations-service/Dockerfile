FROM openjdk:17
COPY build/libs/legacy-aggregations-service-0.0.1-SNAPSHOT.jar /app/aggregations-service-0.0.1-SNAPSHOT.jar
WORKDIR /app

ENV JAVA_OPTS="-Xms2G -Xmx2G"
CMD ["bash", "-c", "java $JAVA_OPTS -jar aggregations-service-0.0.1-SNAPSHOT.jar"]