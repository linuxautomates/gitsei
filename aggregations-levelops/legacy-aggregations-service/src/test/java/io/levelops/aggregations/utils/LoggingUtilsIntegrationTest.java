package io.levelops.aggregations.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class LoggingUtilsIntegrationTest {
    @Test
    public void testWorkitemsMetadata() {
        LoggingUtils.setupThreadLocalContext("messageId", "tenantId", "integrationType", "integrationId");
        log.info("This is a test log! " + 123);
    }
}
