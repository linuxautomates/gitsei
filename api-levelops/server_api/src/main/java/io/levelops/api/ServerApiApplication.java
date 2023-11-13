package io.levelops.api;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ForkJoinPool;

@EnableAsync
@EnableCaching
@SpringBootApplication
@ComponentScan({
        "io.levelops.auth",
        "io.levelops.api.model",
        "io.levelops.commons",
        "io.levelops.api.controllers",
        "io.levelops.api.config",
        "io.levelops.api.services",
        "io.levelops.services",
        "io.levelops.preflightchecks",
        "io.levelops.commons.databases.services",
        "io.levelops.faceted_search",
        "io.harness.accesscontrol.acl.api",
        "io.levelops.contributor"
})
@Log4j2
public class ServerApiApplication {
    public static void main(String[] args) {
        log.info("com.sun.jndi.ldap.object.trustURLCodebase=" + System.getProperty("com.sun.jndi.ldap.object.trustURLCodebase"));
        log.info("CPU Cores: " + Runtime.getRuntime().availableProcessors());
        log.info("CommonPool Parallelism: " + ForkJoinPool.commonPool().getParallelism());
        log.info("CommonPool Common Parallelism: " + ForkJoinPool.getCommonPoolParallelism());
        SpringApplication.run(ServerApiApplication.class, args);
    }
}
