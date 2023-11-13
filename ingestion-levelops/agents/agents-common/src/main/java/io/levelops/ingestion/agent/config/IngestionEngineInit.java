package io.levelops.ingestion.agent.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.engine.controllers.TestController;
import io.levelops.ingestion.sinks.StorageDataSink;

@Configuration
public class IngestionEngineInit {
    
    @Autowired
    public void registerStorageDataSink(IngestionEngine ingestionEngine,
                                        StorageDataSink storageDataSink) {
        ingestionEngine.add("StorageSink", storageDataSink);
    }

    @Autowired
    public void testController(IngestionEngine ingestionEngine,
                               @Value("${env:prod}") String envName) {
        if ("test".equalsIgnoreCase(envName)) {
            ingestionEngine.add("TestController", new TestController(5000));
        }
    }
}
