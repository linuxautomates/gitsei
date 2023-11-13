package io.levelops.ingestion.agent;

import io.levelops.commons.io.RollingOutputStream;
import io.levelops.ingestion.agent.utils.RollingLogUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableConfigurationProperties
public class SatelliteAgentApplication {

    private static final int ROLLING_LOG_SIZE_IN_BYTES = 256 * 1024;

    public static RollingOutputStream rollingLog = null;

    public static void main(String[] args) {
        SatelliteAgentApplication.rollingLog = RollingLogUtils.initRollingLog(ROLLING_LOG_SIZE_IN_BYTES);
        SpringApplication.run(SatelliteAgentApplication.class, args);
    }

}
