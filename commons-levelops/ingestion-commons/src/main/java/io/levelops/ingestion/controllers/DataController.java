package io.levelops.ingestion.controllers;

import io.levelops.ingestion.components.IngestionComponent;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;

public interface DataController<Q extends DataQuery> extends IngestionComponent {

    String COMPONENT_TYPE = "DataController";

    ControllerIngestionResult ingest(JobContext jobContext, Q query) throws IngestException;

    default ControllerIngestionResult ingest(JobContext jobContext, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        return ingest(jobContext, query);
    }

    Q parseQuery(Object o);

    @Override
    default String getComponentType() {
        return COMPONENT_TYPE;
    }

}
