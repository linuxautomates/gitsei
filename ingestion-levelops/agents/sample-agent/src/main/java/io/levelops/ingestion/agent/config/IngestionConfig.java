package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.controllers.SimpleDataController;
import io.levelops.ingestion.data.DataFunctions;
import io.levelops.ingestion.data.VoidQuery;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.TestSink;
import io.levelops.ingestion.sources.DelayFilter;
import io.levelops.ingestion.sources.TestDataSource;
import io.levelops.ingestion.sources.TransformationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class IngestionConfig {

    @Bean
    public IngestionEngine ingestionEngine() {
        IngestionEngine ingestionEngine = new IngestionEngine(2, null);
        TestSink<String> sink1 = TestSink.forClass(String.class);
        TestSink<String> sink2 = TestSink.forClass(String.class);
        TestSink<String> sink3 = TestSink.forClass(String.class);
//        TransformationPipe<Integer, String> pipe1 = TransformationPipe.<Integer, String>builder()
//                .outputSink(sink1)
//                .basicDataTransform(String.class, i -> String.format("Got %d!", i))
//                .build();
        TestDataSource<Integer> testSource = TestDataSource.of(Integer.class, 10, 20, 30, 40);
        DelayFilter<Integer, VoidQuery> delayFilter = DelayFilter.<Integer, VoidQuery>builder()
                .inputSource(testSource)
                .delay(10)
                .delayUnit(TimeUnit.SECONDS)
                .build();
        TransformationFilter<Integer, VoidQuery, String, VoidQuery> filter1 = TransformationFilter.<Integer, VoidQuery, String, VoidQuery>builder()
                .inputSource(delayFilter)
                .transformData(DataFunctions.basicDataTransform(String.class, i -> String.format("Got %d!", i)))
                .build();
        ingestionEngine.registerIngestionComponent(sink1);
        ingestionEngine.registerIngestionComponent(sink2);
        ingestionEngine.registerIngestionComponent(sink3);
//        ingestionEngine.registerIngestionComponent(pipe1);
        ingestionEngine.registerIngestionComponent(filter1);
        ingestionEngine.registerIngestionComponent(testSource);
        ingestionEngine.registerIngestionComponent(SimpleDataController.<String>builder()
                .sink(sink1)
                .source(filter1)
                .build());
        return ingestionEngine;
    }

}
