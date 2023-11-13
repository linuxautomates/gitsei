package io.levelops.integrations.pagerduty.models;

import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class PagerDutyServiceTest {
    @Test
    public void deserializationTest() throws IOException {
        PagerDutyService object = ResourceUtils.getResourceAsObject("integrations/pagerduty/service.json", PagerDutyService.class);
        Assertions.assertThat(object.getCreatedAt().getEpochSecond()).isEqualTo(1579819946L);
        Assertions.assertThat(object.getIngestionDataType()).isEqualTo(PagerDutyIngestionDataType.SERVICE);
        Assertions.assertThat(object.getUpdatedAt()).isEqualTo(1579819946L);
        Assertions.assertThat(object.getId()).isEqualTo("PVE8AWA");
        Assertions.assertThat(object.getName()).isEqualTo("Service1 - API");
    }
    
}
