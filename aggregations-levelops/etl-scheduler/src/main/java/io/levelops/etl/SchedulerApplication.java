package io.levelops.etl;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "io.levelops.commons",
        "io.levelops.aggregations_shared.config",
        "io.levelops.aggregations_shared.database",
        "io.levelops.aggregations_shared.utils",
        "io.levelops.etl",
        "io.levelops.commons.databases.services",
})
@Log4j2
public class SchedulerApplication {
    public static void main(String[] args) {
        log.info("Memory: free={}MB, total={}MB, max={}MB", Runtime.getRuntime().freeMemory() / 1024 / 1024, Runtime.getRuntime().totalMemory() / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024);
        new SpringApplication(SchedulerApplication.class).run(args);
    }
}
