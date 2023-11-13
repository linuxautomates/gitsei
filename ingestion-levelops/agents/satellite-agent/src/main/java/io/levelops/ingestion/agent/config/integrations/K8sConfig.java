package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.agent.ingestion.k8s.K8sCreatePodController;
import io.levelops.ingestion.agent.ingestion.k8s.K8sExecController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.k8s.client.K8sClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class K8sConfig {

    @Bean
    public K8sClient k8SClient() {
        return new K8sClient();
    }

    @Bean
    public K8sExecController k8sExecController(IngestionEngine ingestionEngine, ObjectMapper objectMapper, K8sClient client, StorageDataSink storageDataSink) {
        return ingestionEngine.add("K8sExecController", new K8sExecController(objectMapper, client, storageDataSink));
    }

    @Bean
    public K8sCreatePodController k8sCreatePodController(IngestionEngine ingestionEngine, ObjectMapper objectMapper, K8sClient client, StorageDataSink storageDataSink) {
        return ingestionEngine.add("K8sCreatePodController", new K8sCreatePodController(objectMapper, client, storageDataSink));
    }

}
