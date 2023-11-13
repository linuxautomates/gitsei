package io.levelops.aggregations.parsers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.data.LogEntityWrapper;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Log4j2
@Service
public class JobDtoParser {

    private final Storage storage;
    private final ObjectMapper objectMapper;

    @Autowired
    public JobDtoParser(@NonNull Storage storage, @NonNull ObjectMapper objectMapper) {
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    public <C> boolean applyToResultsWithLog(String customer, String dataType, Class<C> cContentTypeClass,
                                             TriggerResults results, Consumer<LogEntityWrapper<C>> consumer,
                                             List<Callable<Boolean>> callables) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(LogEntityWrapper.class, cContentTypeClass);
        BiConsumer<StorageContent<ListResponse<LogEntityWrapper<C>>>, JobDTO> biConsumer = (response, jobDto) -> streamRecords(response)
                .map(data -> data.toBuilder()
                        .data(objectMapper.convertValue(data.getData(), cContentTypeClass))
                        .build())
                .forEach(consumer);
        return this.applyToResults(customer, dataType, javaType, results, biConsumer, callables);
    }

    public <C> boolean applyToResults(String customer, String dataType, Class<C> cContentTypeClass,
                                      TriggerResults results, Consumer<C> consumer,
                                      List<Callable<Boolean>> callables) {
        JavaType javaType = objectMapper.constructType(cContentTypeClass);
        BiConsumer<StorageContent<ListResponse<C>>, JobDTO> biConsumer = (response, jobDto) -> streamRecords(response).forEach(consumer);
        return this.applyToResults(customer, dataType, javaType, results, biConsumer, callables);
    }

    public <C> boolean applyToResults(String customer, String dataType, Class<C> cContentTypeClass,
                                      TriggerResults results, BiConsumer<C, JobDTO> consumer,
                                      List<Callable<Boolean>> callables) {
        JavaType javaType = objectMapper.constructType(cContentTypeClass);
        BiConsumer<StorageContent<ListResponse<C>>, JobDTO> biConsumer = (response, jobDto) -> streamRecords(response)
                .forEach(item -> consumer.accept(item, jobDto));
        return this.applyToResults(customer, dataType, javaType, results, biConsumer, callables);
    }

    private <C> boolean applyToResults(String customer, String dataType, JavaType cContentTypeClass,
                                       TriggerResults results, BiConsumer<StorageContent<ListResponse<C>>, JobDTO> consumer,
                                       List<Callable<Boolean>> callables) {
        AtomicBoolean success = new AtomicBoolean(true);
        results.getJobs().forEach(jobDTO -> {
            if (!success.get()) {
                return;
            }
            try {
                ListResponse<StorageResult> storageResult = objectMapper.convertValue(jobDTO.getResult(),
                        ListResponse.typeOf(objectMapper, StorageResult.class));
                for (StorageResult result : storageResult.getRecords()) {
                    if (result.getStorageMetadata() == null || !dataType.equals(result.getStorageMetadata().getDataType())) {
                        continue;
                    }
                    for (GcsDataResult record : result.getRecords()) {
                        StorageContent<ListResponse<C>> response = fetchRecordsFromGcs(record, cContentTypeClass, customer, results.getIntegrationId(), results.getTriggerId(), results.getIterationId());
                        if(response == null){
                            //there is logging inside fetchRecordsFromGcs, no need to log again
                            continue;
                        }
                        consumer.accept(response, jobDTO);
                        for (Callable<Boolean> callable : callables) {
                            success.compareAndSet(true, callable.call());
                            if (!success.get()) {
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Exception handling job data for customer={}, integrationId={}, triggerId={}, iterationId={}: {}",
                        customer, results.getIntegrationId(), results.getTriggerId(), results.getIterationId(), e.getMessage(), e);
                success.compareAndSet(true, false);
            }
        });
        return success.get();
    }

    private <C> StorageContent<ListResponse<C>> fetchRecordsFromGcs(GcsDataResult record, JavaType cContentTypeClass, String customer, String integrationId, String triggerId, String iterationId) throws JsonProcessingException {
        String issuesBucket = record.getBlobId().getBucket();
        String issuesPath = record.getBlobId().getName();
        Long issuesGeneration = record.getBlobId().getGeneration();
        byte[] bytes = null;
        try {
            bytes = storage.readAllBytes(BlobId.of(issuesBucket, issuesPath, issuesGeneration));
        } catch (Exception e) {
            log.warn("Error downloading file customer={}, integrationId={}, triggerId={}, iterationId={}, {}, {}, {}, will skip this file!", customer, integrationId, triggerId, iterationId, issuesBucket, issuesPath, issuesGeneration);
            return null;
        }
        StorageContent<ListResponse<C>> response = objectMapper.readValue(new String(bytes),
                StorageContent.getListStorageContentJavaType(objectMapper, cContentTypeClass));
        log.info("Fetched {} records from file {}.", response.getData().getRecords().size(), issuesPath);
        return response;
    }

    private static <C> Stream<C> streamRecords(StorageContent<ListResponse<C>> response) {
        if (response == null || response.getData() == null || response.getData().getRecords() == null) {
            return Stream.empty();
        }
        return response.getData().getRecords().stream();
    }
}
