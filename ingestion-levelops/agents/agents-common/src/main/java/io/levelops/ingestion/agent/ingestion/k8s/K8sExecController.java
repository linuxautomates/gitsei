package io.levelops.ingestion.agent.ingestion.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.custom.k8s.models.K8sExecQuery;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.k8s.client.K8sClient;
import io.levelops.integrations.k8s.models.K8sExecOutput;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class K8sExecController implements DataController<K8sExecQuery> {

    private static final String EXEC_DATA_TYPE = "exec";
    private static final Integer DEFAULT_TIMEOUT_IN_MIN = 1;
    private final ObjectMapper objectMapper;
    private final K8sClient client;
    private final StorageStrategy storageStrategy;

    public K8sExecController(ObjectMapper objectMapper, K8sClient client, StorageDataSink storageDataSink) {
        this.objectMapper = objectMapper;
        this.client = client;

        storageStrategy = new StorageStrategy(objectMapper, storageDataSink);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, K8sExecQuery query) throws IngestException {
        int timeoutInMinutes = MoreObjects.firstNonNull(query.getTimeoutInMinutes(), DEFAULT_TIMEOUT_IN_MIN);
        try {
            K8sClient.K8sExecOutput exec = client.exec(query.getNamespace(), query.getPodName(), query.getContainer(), query.getCommand(), timeoutInMinutes);

            K8sExecOutput data = K8sExecOutput.builder()
                    .output(exec.getOutput())
                    .error(exec.getError())
                    .build();

            return storageStrategy.storeOnePage(
                    query.getIntegrationKey(),
                    IntegrationType.CUSTOM.toString(),
                    EXEC_DATA_TYPE,
                    jobContext.getJobId(),
                    ListResponse.of(List.of(data)),
                    null /* not paginated */,
                    null);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new IngestException("Failed to run exec command", e);
        }
    }

    @Override
    public K8sExecQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, K8sExecQuery.class);
    }
}
