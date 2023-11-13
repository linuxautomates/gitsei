package io.levelops.ingestion.strategies;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.EntityWithLogs;
import io.levelops.ingestion.data.LogEntityWrapper;
import io.levelops.ingestion.data.LogMetadata;
import io.levelops.ingestion.data.LogWithMetadata;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.exceptions.PushException;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class LogEntityStorageStrategy implements IStorageStrategy {

    private final StorageStrategy delegate;
    private final StorageDataSink storageDataSink;

    @Builder
    public LogEntityStorageStrategy(ObjectMapper objectMapper, StorageDataSink storageDataSink) {
        this.delegate = new StorageStrategy(objectMapper, storageDataSink);
        this.storageDataSink = storageDataSink;
    }

    @Override
    public <D> StorageResult storeOnePage(IntegrationKey integrationKey,
                                          String integrationType,
                                          String dataType,
                                          String jobId,
                                          ListResponse<D> data,
                                          @Nullable Integer pageNumber,
                                          @Nullable String fileNamePrefix) throws IngestException {
        List<LogEntityWrapper<?>> logEntityWrappers = getLogEntityWrappers(jobId, integrationKey, integrationType,
                dataType, data.getRecords());
        return delegate.storeOnePage(integrationKey, integrationType, dataType, jobId,
                ListResponse.of(logEntityWrappers), pageNumber, fileNamePrefix);
    }

    private <D> List<LogEntityWrapper<?>> getLogEntityWrappers(String jobId, IntegrationKey integrationKey,
                                                               String integrationType,
                                                               String dataType, List<D> data) {
        return getLogEntityWrappers(jobId, integrationKey, integrationType, dataType, data.stream()
                .map(d -> {
                    if (!(d instanceof EntityWithLogs)) {
                        throw new RuntimeException("Could not store data: EntityWithLogs expected, got: " + d.getClass());
                    }
                    return (EntityWithLogs<?>) d;
                }));
    }

    private <D extends EntityWithLogs<?>> List<LogEntityWrapper<?>> getLogEntityWrappers(
            String jobId,
            IntegrationKey integrationKey,
            String integrationType,
            String dataType,
            Stream<D> entityWithLogsStream) {
        return entityWithLogsStream
                .map(d -> {
                    List<LogWithMetadata> logWithMetadataList = d.getLogWithMetadata();
                    return LogEntityWrapper.builder()
                            .data(d.getData())
                            .metadata(CollectionUtils.emptyIfNull(logWithMetadataList).stream()
                                    .map(logWithMetadata -> storeLogAndGetMetadata(jobId, integrationKey,
                                            integrationType, dataType, logWithMetadata))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ImmutablePair<String, LogMetadata> storeLogAndGetMetadata(String jobId, IntegrationKey integrationKey,
                                                                      String integrationType,
                                                                      String dataType,
                                                                      LogWithMetadata logWithMetadata) {
        try {
            BlobId blobId = storeLog(logWithMetadata.getId(), logWithMetadata.getLog(), integrationKey,
                    integrationType, dataType, jobId);
            return ImmutablePair.of(logWithMetadata.getId(), LogMetadata.builder()
                    .logBucket(blobId.getBucket())
                    .logLocation(blobId.getName())
                    .metadata(logWithMetadata.getMetadata())
                    .build());
        } catch (PushException e) {
            log.warn("getLogMetadata: error pushing log for: " + logWithMetadata.getId(), e);
            return null;
        }
    }

    private BlobId storeLog(String id, byte[] content, IntegrationKey integrationKey, String integrationType,
                            String dataType, String jobId)
            throws PushException {
        final StorageResult storageResult = storageDataSink.pushOne(BasicData.of(StorageData.class,
                StorageData.builder()
                        .content(content)
                        .contentType("text/plain")
                        .integrationKey(integrationKey)
                        .integrationType(integrationType)
                        .dataType(dataType)
                        .relativePath(dataType + "/logs/" + id + ".log")
                        .jobId(jobId)
                        .build()));
        return storageResult.getRecords().stream().findFirst().map(GcsDataResult::getBlobId).orElse(null);
    }
}
