package io.levelops.internal_api;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.ForkJoinPool;

@Log4j2
@SpringBootApplication
@ComponentScan({
        "io.levelops.commons",
        "io.levelops.commons.databases.services",
        "io.levelops.internal_api"
})
public class InternalApiApplication {

    public static void main(String[] args) {
        log.info("CPU Cores: " + Runtime.getRuntime().availableProcessors());
        log.info("CommonPool Parallelism: " + ForkJoinPool.commonPool().getParallelism());
        log.info("CommonPool Common Parallelism: " + ForkJoinPool.getCommonPoolParallelism());
        SpringApplication.run(InternalApiApplication.class, args);
    }

}
