package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert.Body;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class PagerDutyTransitionalEntityTest {
    @Test
    public void deserializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/alert_agg_row.json");
        PagerDutyEntity tmp = mapper.readValue(json, PagerDutyTransitionalEntity.class);
        
        PagerDutyAlert object = (PagerDutyAlert) mapper.convertValue(tmp, tmp.getIngestionDataType().getIngestionDataTypeClass());
        Assert.assertEquals(1581121876L, object.getCreatedAt().toInstant().getEpochSecond());
        Assert.assertEquals(1581208276L, object.getResolvedAt().toInstant().getEpochSecond());
        Assert.assertEquals("P2LYZHY", object.getId());
        Assert.assertEquals(PagerDutyAlert.Severity.critical, object.getSeverity());
        Assert.assertEquals("triggered", object.getStatus());
        Assert.assertNotNull(object.getBody());
        Assert.assertNotNull(object.getBody());
        Assertions.assertThat("{\"test\":\"ok\"}").isEqualTo(object.getBody());
    }

    @Test
    public void serializeTest() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .setSerializationInclusion(Include.NON_ABSENT);
        var d = mapper.writeValueAsString(
            PagerDutyAlert.builder()
                .id("P2LYZHY")
                .alertKey("alertKey")
                .body("{\"test\":\"ok\"}")
                .status("triggered")
                .severity(PagerDutyAlert.Severity.critical)
                .createdAt(Date.from(Instant.ofEpochMilli(1581121876000L)))
                .resolvedAt(Date.from(Instant.ofEpochMilli(1581208276000L)))
                .build());
        Assert.assertNotNull(d);
        Assert.assertEquals(d, ResourceUtils.getResourceAsString("integrations/pagerduty/alert_agg_row.json"));
    }
}