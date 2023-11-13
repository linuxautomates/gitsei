package io.levelops.aggregations.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Log4j2
public class AutomationRulesEngineTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws JsonProcessingException {
        var objects = List.of(
                "ahajhjka",
                List.of("abc", "xyz", "lmn"),
                Map.of("k1", "v1", "k2", "v2"),
                AutomationRule.builder().name("name1").build(),
                List.of(AutomationRule.builder().name("name1").build(),AutomationRule.builder().name("name2").build())
        );
        for(Object obj : objects) {
            log.info(obj instanceof String);
            log.info(obj instanceof Collection);
            log.info(obj.toString());
            String abcd = MAPPER.writeValueAsString(obj);
            log.info(abcd);
            log.info(MAPPER.writeValueAsString(obj));
            log.info("-------------------------------");
        }
    }
}