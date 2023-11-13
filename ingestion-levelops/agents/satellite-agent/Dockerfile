FROM levelops/java-base:v1.0.51_jdk-17.0.8.1
LABEL org.opencontainers.image.authors="Propelo (maxime@propelo.ai, ivan@propelo.ai)"
COPY build/libs/satellite-agent-0.0.1-SNAPSHOT.jar /levelops/satellite-agent.jar
# COPY build/reports/dependency-license /licenses
# We need curl to allow customers to run health-checks (for example in ECS)
# RUN apt-get update && apt-get install -y curl # sometimes getting errors with this ?
WORKDIR /levelops
EXPOSE 8080

# Create user and group
ARG user=satellite
ARG group=satellite
ARG uid=1000
ARG gid=1000
RUN groupadd -g ${gid} ${group}
RUN useradd -u ${uid} -g ${group} -s /bin/sh -m ${user}

# Switch to app user
USER ${uid}:${gid}

ARG BUILD_VERSION
ENV BUILD_VERSION=${BUILD_VERSION:-Unknown}
ENV JVM_OPTS "-XX:MinRAMPercentage=20.0 -XX:MaxRAMPercentage=90.0"
ENV SERVICE_PORT "8080"
ENV LEVELOPS_DEBUG false
ENV ENCRYPTION_OPT="--spring.profiles.active=encrypted"
ENV LOGGER_CONFIG "-Dlogging.level.io.levelops=DEBUG"
ENV LC_ALL='C.UTF-8'
CMD ["bash", "-c", "if [[ \"$LEVELOPS_DEBUG\" != \"true\" ]] ; then LOGGER_CONFIG=\"\"; fi; if [[ -z \"$ENCRYPTION_PASSWORD\" ]] ; then ENCRYPTION_OPT=\"\"; fi; java $JVM_OPTS $LOGGER_CONFIG -Dserver.port=$SERVICE_PORT -jar satellite-agent.jar $ENCRYPTION_OPT" ]

# Build: docker build -t <image name> .
# (Specify version when building with: docker build -t <image name> . --build-arg BUILD_VERSION=123)
# Run:   docker run --rm --name sat-dev -d -v /absolute/path/to/host/config.yml:/levelops/config.yml <image name>

# For dev purposes, see:
# ./build-dev-satellite-locally.sh
# ./run-dev-satellite.sh ~/Downloads/satellite.yml
# ./stop-dev-satellite.sh