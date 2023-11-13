package io.levelops.ingestion.data;

import java.util.List;

public interface EntityWithLogs<D> {

    D getData();

    List<LogWithMetadata> getLogWithMetadata();

}

