package io.levelops.ingestion.agent.ingestion.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;

// import io.fabric8.kubernetes.client.KubernetesClientException;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.custom.k8s.models.K8sCreatePodQuery;
import io.levelops.ingestion.integrations.custom.k8s.models.K8sCreatePodQuery.PodDeletion;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.k8s.client.K8sClient;
import io.levelops.integrations.k8s.models.K8sExecOutput;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.util.List;

@Log4j2
public class K8sCreatePodController implements DataController<K8sCreatePodQuery> {

    private static final String EXEC_DATA_TYPE = "exec";
    private static final Integer DEFAULT_TIMEOUT_IN_MIN = 1;
    private final ObjectMapper objectMapper;
    private final K8sClient client;
    private final StorageStrategy storageStrategy;

    public K8sCreatePodController(ObjectMapper objectMapper, K8sClient client, StorageDataSink storageDataSink) {
        this.objectMapper = objectMapper;
        this.client = client;

        storageStrategy = new StorageStrategy(objectMapper, storageDataSink);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, K8sCreatePodQuery query) throws IngestException {
        String namespace = query.getNamespace();
        int timeoutInMinutes = MoreObjects.firstNonNull(query.getTimeoutInMinutes(), DEFAULT_TIMEOUT_IN_MIN);
        String podName = null;
        boolean successful = false;
        try {
            podName = client.createPod(namespace, query.getYamlResource(), query.getNameSuffix());

            log.info("Waiting for pod '{}' to terminate... (timeout={}min)", podName, timeoutInMinutes);
            successful = client.waitForPodTermination(namespace, podName, timeoutInMinutes);
            if (!successful) {
                throw new IngestException("Pod did not terminate successfully: " + podName);
            }

            String output = client.getPodLogs(namespace, podName);

            K8sExecOutput data = K8sExecOutput.builder()
                    .output(output)
                    .build();

            return storageStrategy.storeOnePage(
                    query.getIntegrationKey(),
                    IntegrationType.CUSTOM.toString(),
                    EXEC_DATA_TYPE,
                    jobContext.getJobId(),
                    ListResponse.of(List.of(data)),
                    null /* not paginated */,
                    null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IngestException("Failed to create pod and retrieve logs", e);
        } finally {
                deletePodIfNeeded(namespace, podName, successful, query.getPodDeletion());
        }
    }

    private void deletePodIfNeeded(String namespace, @Nullable String podName, boolean successful, @Nullable PodDeletion podDeletion) {
        if (podName == null || podDeletion == null) {
            return;
        }
        if (podDeletion == PodDeletion.NEVER) {
            return;
        }
        if (podDeletion == PodDeletion.ONLY_IF_SUCCESSFUL && !successful) {
            return;
        }
        // try {
            boolean deleted = client.deletePod(namespace, podName);
            if (deleted) {
                log.info("Deleted pod '{}' in namespace '{}' successfully", podName, namespace);
            } else {
                log.warn("Could not delete pod '{}' in namespace '{}'", podName, namespace);
            }
        // } 
        // catch (KubernetesClientException e) {
        //     log.warn("Failed to delete pod '{}' in namespace '{}'", podName, namespace, e);
        // }
    }

    @Override
    public K8sCreatePodQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, K8sCreatePodQuery.class);
    }

}
