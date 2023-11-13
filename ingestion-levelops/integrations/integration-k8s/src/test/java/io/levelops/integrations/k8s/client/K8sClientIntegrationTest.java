package io.levelops.integrations.k8s.client;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class K8sClientIntegrationTest {

    @Test
    public void testExec() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        K8sClient k8sClient = new K8sClient();
        System.out.println("init");
        K8sClient.K8sExecOutput exec = k8sClient.exec("default", "nginx-6db489d4b7-6724k", null, "ls | sort -r ; echo \"hello\" 1>&2;", 1);
        System.out.println("--- Output:\n" + exec.getOutput());
        System.out.println("--- Error:\n" + exec.getError());
    }

    @Test
    public void listPods() {
        KubernetesClient client = new DefaultKubernetesClient();
        PodList list = client.pods().list();
        list.getItems().forEach(pod -> System.out.println(pod.getMetadata().getNamespace() + " " + pod.getStatus().getPodIP() + " " + pod.getMetadata().getName()));
    }

    @Test
    public void name() {
        DefaultKubernetesClient client = new DefaultKubernetesClient();
        String yml = "apiVersion: v1\n" +
                "kind: Pod\n" +
                "metadata:\n" +
                "  name: bare-pod\n" +
                "  labels:\n" +
                "    levelops-job-id: job-123\n" +
                "spec:\n" +
                "  containers:\n" +
                "  - name: bare-pod-container\n" +
                "    image: debian\n" +
                "    command: [\"/bin/sh\"]\n" +
                "    args: [\"-c\", \"sleep 2 && echo done\"]\n" +
                "  restartPolicy: Never";

//        yml =   "  containers:\n" +
//                "  - name: bare-pod-container\n" +
//                "    image: debian\n" +
//                "    command: [\"/bin/sh\"]\n" +
//                "    args: [\"-c\", \"sleep 2 && echo done\"]\n" +
//                "  restartPolicy: Never";

        Object unmarshal = Serialization.unmarshal(new ByteArrayInputStream(yml.getBytes()));
        DefaultObjectMapper.prettyPrint(unmarshal);

        List<HasMetadata> resources = client.load(new ByteArrayInputStream(yml.getBytes())).get();

        if (resources.isEmpty()) {
            System.err.println("No resources loaded");
            return;
        }
        HasMetadata resource = resources.get(0);
        DefaultObjectMapper.prettyPrint(resource);

        if (resource instanceof Pod) {
            Pod pod = (Pod) resource;
            var name = pod.getMetadata().getName();
            DefaultObjectMapper.prettyPrint(name);
        }
    }

    @Test
    public void name2() {
        DefaultKubernetesClient client = new DefaultKubernetesClient();
        client.pods().inNamespace("maxime-dev").withName("playbook-pod-864c3dc7-8607-4b0a-919a-2651eb014f09").delete();
    }
}