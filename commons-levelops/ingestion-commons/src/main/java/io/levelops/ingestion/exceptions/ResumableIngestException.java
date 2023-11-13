package io.levelops.ingestion.exceptions;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class ResumableIngestException extends IngestException {

    private final String customMessage;
    private final ControllerIngestionResult result; // partial data up that was ingested before the exception
    private final Throwable error;
    private final Map<String, Object> intermediateState;

    private ResumableIngestException(String customMessage, ControllerIngestionResult result, Throwable error, Map<String, Object> intermediateState) {
        super(String.format(StringUtils.defaultIfBlank(customMessage, "Resumable error") + " (result=%s, intermediate_state=%s)", result != null, intermediateState != null), error);
        this.customMessage = customMessage;
        this.result = result;
        this.error = error;
        this.intermediateState = intermediateState;
    }

}
