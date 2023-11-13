package io.levelops.ingestion.controllers;

import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.VoidQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.DataSink;
import io.levelops.ingestion.sinks.SinkIngestionResult;
import io.levelops.ingestion.sources.DataSource;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Builder
@Log4j2
public class SimpleDataController<T> implements DataController<VoidQuery> {

    DataSource<T, VoidQuery> source;
    DataSink<T, ? extends SinkIngestionResult> sink;

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, VoidQuery query) throws IngestException {
        Data<T> data = source.fetchOne(null);
        sink.pushOne(data);

        log.info("Ingested {}", data);
        return null;
    }

    @Override
    public VoidQuery parseQuery(Object o) {
        return null;
    }
}
