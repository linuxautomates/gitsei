package io.levelops.ingestion.services;

import java.io.IOException;
import java.util.Optional;

public interface IngestionCachingService {

    boolean isEnabled();

    Optional<String> read(String company, String integrationId, String objectKey);

    void write(String company, String integrationId, String objectKey, String objectValue) throws IOException;

}
