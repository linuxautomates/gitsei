package io.levelops.ingestion.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

@Log4j2
public class StorageStrategy implements IStorageStrategy {

    private final ObjectMapper objectMapper;
    private final StorageDataSink storageDataSink;

    @Builder
    public StorageStrategy(ObjectMapper objectMapper, StorageDataSink storageDataSink) {
        this.objectMapper = objectMapper;
        this.storageDataSink = storageDataSink;
    }

    public static String getRelativePathPrefix(String dataType) {
        return dataType + "/";
    }

    private static String getFileName(@Nullable String fileNamePrefix, String dataType, @Nullable Integer pageNumber) {
        return StringUtils.defaultString(fileNamePrefix) + dataType + (pageNumber != null ? "." + pageNumber : "") + ".json";
    }

    private static String getRelativePath(String dataType, @Nullable Integer pageNumber, @Nullable String fileNamePrefix) {
        return getRelativePathPrefix(dataType) + getFileName(fileNamePrefix, dataType, pageNumber);
    }

    public <D> StorageResult storeOnePage(
            IntegrationKey integrationKey,
            String integrationType,
            String dataType,
            String jobId,
            ListResponse<D> data,
            @Nullable Integer pageNumber,
            @Nullable String fileNamePrefix) throws IngestException {

        StorageMetadata storageMetadata = StorageMetadata.builder()
                .key(integrationKey)
                .dataType(dataType)
                .integrationType(integrationType)
                .pageNumber(pageNumber)
                .build();

        StorageContent<?> content = StorageContent.builder()
                .data(data)
                .storageMetadata(storageMetadata)
                .build();

        StorageResult result;
        try {
            result = storageDataSink.pushOne(BasicData.of(StorageData.class, StorageData.builder()
                    .integrationKey(integrationKey)
                    .jobId(jobId)
                    .relativePath(getRelativePath(dataType, pageNumber, fileNamePrefix))
                    .jsonContent(objectMapper.writeValueAsString(content))
                    .build()));
        } catch (JsonProcessingException e) {
            throw new IngestException(String.format("Failed to store data for tenant=%s, integrationId=%s, dataType=%s, jobId=%s", integrationKey.getTenantId(), integrationKey.getIntegrationId(), dataType, jobId), e);
        }

        return result.toBuilder()
                .storageMetadata(storageMetadata)
                .build();
    }

}
