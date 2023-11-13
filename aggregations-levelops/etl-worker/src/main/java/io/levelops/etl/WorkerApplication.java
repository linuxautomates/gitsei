package io.levelops.etl;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = {
        "io.levelops.commons",
        "io.levelops.commons.databases.services",
        "io.levelops.aggregations_shared.config",
        "io.levelops.aggregations_shared.database",
        "io.levelops.aggregations_shared.services",
        "io.levelops.aggregations_shared.helpers",
        "io.levelops.aggregations_shared.utils",
        "io.levelops.aggregations.services",
        "io.levelops.etl",
        "io.levelops.repomapping"
})
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@Log4j2
public class WorkerApplication {

    public static void main(String[] args) {
        log.info("Memory: free={}MB, total={}MB, max={}MB", Runtime.getRuntime().freeMemory() / 1024 / 1024, Runtime.getRuntime().totalMemory() / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024);
        new SpringApplication(WorkerApplication.class).run(args);
    }
}

