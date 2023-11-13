package io.levelops.aggregations;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableIntegration
@SpringBootApplication
@ComponentScan(basePackages = {
        "io.levelops.commons",
        "io.levelops.aggregations.config",
        "io.levelops.aggregations.controllers",
        "io.levelops.aggregations.helpers",
        "io.levelops.aggregations.services",
        "io.levelops.commons.databases.services",
        "io.levelops.faceted_search.services",
        "io.levelops.aggregations_shared.services",
        "io.levelops.aggregations_shared.helpers",
        "io.levelops.aggregations_shared.config"
})
@Log4j2
public class AggregationsApplication {
    public static void main(String[] args) {
        log.info("Memory: free={}MB, total={}MB, max={}MB", Runtime.getRuntime().freeMemory() / 1024 / 1024, Runtime.getRuntime().totalMemory() / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024);
        new SpringApplication(AggregationsApplication.class).run(args);
    }
}