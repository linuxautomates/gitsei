package io.levelops.commons.databases.models.database.automation_rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AutomationRuleTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        List<Criterea> critereas = List.of(
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1", "name 2"))
                        .build(),
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1", "name 2"))
                        .build()
        );
        AutomationRule expected = AutomationRule.builder()
                .id(UUID.randomUUID())
                .name("PR Infra Changes")
                .description("Catch Infra changes in PRs")
                .source("http://jenkins.com")
                .owner("viraj@levelops.io")
                .objectType(ObjectType.JIRA_ISSUE)
                .critereas(critereas)
                .createdAt((new Date()).toInstant())
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        AutomationRule actual = MAPPER.readValue(serialized, AutomationRule.class);
        assertThat(actual).isEqualTo(expected);
    }
}