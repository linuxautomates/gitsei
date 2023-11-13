package io.levelops.ingestion.sources;

import io.levelops.ingestion.components.IngestionComponent;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;

import java.util.stream.Stream;

public interface DataSource<D, Q extends DataQuery> extends IngestionComponent {

    Data<D> fetchOne(Q query) throws FetchException;

    /**
     * @deprecated please implement {@link io.levelops.ingestion.sources.DataSource#fetchMany(io.levelops.ingestion.models.JobContext, Q)} instead
     *
     */
    @Deprecated
    Stream<Data<D>> fetchMany(Q query) throws FetchException;

    default Stream<Data<D>> fetchMany(JobContext jobContext, Q query) throws FetchException {
        return fetchMany(query);
    }

    default Stream<Data<D>> fetchMany(JobContext jobContext, Q query, IntermediateStateUpdater intermediateStateUpdater) throws FetchException {
        return fetchMany(jobContext, query);
    }

    @Override
    default String getComponentType() {
        return "DataSource";
    }

}
