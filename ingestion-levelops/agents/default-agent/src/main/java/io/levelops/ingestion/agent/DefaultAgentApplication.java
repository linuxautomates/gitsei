package io.levelops.ingestion.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {
                "io.levelops.ingestion.agent.config",
                "io.levelops"
        },
        exclude = {
                DataSourceAutoConfiguration.class
        }
)
public class DefaultAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DefaultAgentApplication.class, args);
    }

}
