package io.levelops.integrations.k8s.client;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Log4j2
public class K8sClient {

    private static final int MAX_OUTPUT_SIZE_IN_BYTES = 20 * 1024 * 1024; // 20 MBi
    private static final String POD_SUCCEEDED_PHASE = "Succeeded";
    private static final String POD_FAILED_PHASE = "Failed";
    private static final Predicate<Pod> IS_POD_TERMINATED = pod -> Set.of(POD_SUCCEEDED_PHASE, POD_FAILED_PHASE).contains(pod.getStatus().getPhase());

    private final KubernetesClient client;

    /**
     * Create Kubernetes client configured based on environment:
     * - System properties
     * - Environment variables
     * - Kube config file
     * - Service account token & mounted CA certificate
     * https://github.com/fabric8io/kubernetes-client/tree/v4.10.2
     */
    public K8sClient() {
        KubernetesClient client;
        try {
            log.debug("Initalizing Kubernetes client...");
            client = new DefaultKubernetesClient();
            log.info("Kubernetes client initialized.");
        } catch (KubernetesClientException e) {
            log.warn("Failed to initialize K8s client: {}", e.getMessage());
            log.debug("K8s error", e);
            client = null;
        }
        this.client = client;
    }

    private KubernetesClient getClient() {
        if (client == null) {
            throw new IllegalStateException("K8s client is not initialized");
        }
        return client;
    }

    @Value
    public static class K8sExecOutput {
        String output;
        String error;
    }

    /**
     * Execute a command on an existing pod
     *
     * @param namespace        e.g. "default"
     * @param podName          e.g. "nginx-4217019353-k5sn9"
     * @param container        optional
     * @param command          "echo foo"
     * @param timeoutInMinutes how long to wait for the command to run
     * @return output string and exit value
     */
    public K8sExecOutput exec(String namespace, String podName, @Nullable String container, String command, int timeoutInMinutes) throws InterruptedException, TimeoutException, ExecutionException {
        // TODO make this configurable?
        String[] commandFragments = {"sh", "-c", command};


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        CompletableFuture<K8sExecOutput> data = new CompletableFuture<>();
        try (ExecWatch execWatch = getClient().pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(container)
                .readingInput(null)
                .writingOutput(outputStream)
                .writingError(errorStream)
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen() {
                        log.debug("K8s Exec: opened");
                    }

                    @Override
                    public void onFailure(Throwable throwable, Response response) {
                        data.completeExceptionally(throwable);
                    }

                    @Override
                    public void onClose(int i, String s) {
                        data.complete(new K8sExecOutput(outputStream.toString(), errorStream.toString()));
                    }
                }).exec(commandFragments)) {
            return data.get(timeoutInMinutes, TimeUnit.MINUTES);
        }
    }

    /**
     * Create Pod based on given yaml resource.
     * @return pod name
     */
    public String createPod(String namespace, String yamlResource, String nameSuffix) {
        List<HasMetadata> resources = client.load(new ByteArrayInputStream(yamlResource.getBytes())).get();
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Could not load resource from Yaml: " + yamlResource);
        }
        HasMetadata resource = resources.get(0);
        if (!(resource instanceof Pod)) {
            throw new IllegalArgumentException("Resource is not a Pod: " + resource);
        }

        Pod pod = (Pod) resource;
        if (StringUtils.isEmpty(pod.getMetadata().getName())) {
            throw new IllegalArgumentException("Pod does not have a name: " + pod);
        }
        if (StringUtils.isNotEmpty(nameSuffix)) {
            pod.getMetadata().setName(pod.getMetadata().getName() + nameSuffix);
        }

        log.info("Creating pod in namespace '{}'", namespace);
        var createdPod = getClient().pods().inNamespace(namespace).create(pod);
        log.info("Created pod '{}' in namespace '{}'", createdPod.getMetadata().getName(), namespace);

        return createdPod.getMetadata().getName();
    }

    /**
     * Wait for pod to terminate.
     * @return True if pod was successful.
     */
    public boolean waitForPodTermination(String namespace, String podName, int timeoutInMinutes) throws InterruptedException {
        var pod = getClient().pods().inNamespace(namespace).withName(podName)
                .waitUntilCondition(IS_POD_TERMINATED,  timeoutInMinutes, TimeUnit.MINUTES);
        return POD_SUCCEEDED_PHASE.equals(pod.getStatus().getPhase());
    }

    public String getPodLogs(String namespace, String podName) {
        return getClient().pods().inNamespace(namespace).withName(podName).getLog();
    }

    public boolean deletePod(String namespace, String podName) {
        return BooleanUtils.isTrue(getClient().pods().inNamespace(namespace).withName(podName).delete().stream().filter(i -> podName.equalsIgnoreCase(i.getName())).findAny().isPresent());
    }

}
