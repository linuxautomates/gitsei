FROM levelops/java-base:latest
COPY build/libs/server_api-0.0.1-SNAPSHOT.jar /usr/src/myapp/server_api-0.0.1-SNAPSHOT.jar
COPY ui/admin_login.html /app/propelo/admin_login.html
COPY ui/welcome_email.html /app/propelo/welcome_email.html
COPY ui/demo_dashboard.json /app/propelo/demo_dashboard.json
WORKDIR /usr/src/myapp

ENV JAVA_OPTS="-Xms2G -Xmx2G"
CMD ["bash", "-c", "java $JAVA_OPTS -jar server_api-0.0.1-SNAPSHOT.jar"]
