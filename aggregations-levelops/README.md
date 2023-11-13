# Aggregations service
Aggregation tasks run here.

## Pre Requirements
* Java
* Gradle
* GCP credentials (json)
* LevelOps artifacts repository access.

## Local Run

### Setup local DB
> Initial DB setup
>
>     mkdir -p ~/data/pg
>     docker run -d --name pg -e POSTGRES_PASSWORD=postgres -e PG_DATA=/opt/pg -v ~/data/pg:/opt/pg -p 5432:5432 postgres

> This setup will retain the db data.
> 
> To start the db after the initial setup:
> 
>     docker start pg

### Build
>     cd project-root
>     ./gradlew clean build  

### Run
>     GOOGLE_CLOUD_PROJECT=levelops-dev GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/gcp_key.json" DB_IP="localhost" DB_PORT="5432" DB_USERNAME="postgres" DB_PASSWORD="postgres" DB_SSL_MODE="disable" java -jar build/libs/aggregations-service-0.0.1-SNAPSHOT.jar