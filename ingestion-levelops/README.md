# ingestion-levelops
Ingestion Framework

# Design Doc

https://levelops.atlassian.net/wiki/spaces/LEV/pages/14942216/Ingestion+Framework+Design+Doc

# Packages

* `ingestion-engine` contains the core ingestion framework (the ingestion engine, and building blocks for data sources and sinks, controllers, etc.)
* `ingestion-agent` contains the building blocks to deploy the ingestion engine as a Spring application (web controllers, etc.)
* `agents/*` is a folder of agent packages containing configured, readily-deployable, agents (i.e. an instance of an ingestion-agent)   
* `control-plane` contains the Control-Plane Spring application
* `integrations/*` is a folder of packages containing clients and SDKs for multiple integrations
