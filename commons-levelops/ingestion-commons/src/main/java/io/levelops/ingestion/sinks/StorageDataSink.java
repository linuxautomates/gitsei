package io.levelops.ingestion.sinks;

import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;


/**
 * Storage Data Sink to abstract writing data to some storage system and return a standardized StorageResult
 */
public interface StorageDataSink extends DataSink<StorageData, StorageResult> {

}
