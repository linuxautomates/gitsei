package io.levelops.integrations.jira.converters;

import com.fasterxml.jackson.databind.JsonNode;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.NormalizedJiraIssue;
import io.levelops.normalization.exceptions.NormalizationException;
import io.levelops.normalization.services.NormalizationService;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueConverterTest {

    @Test
    public void normalize() throws NormalizationException {
        NormalizationService normalizationService = new NormalizationService(DefaultObjectMapper.get());

        JiraIssue jiraIssue = JiraIssue.builder()
                .key("LEV-123")
                .fields(JiraIssueFields.builder()
                        .assignee(JiraUser.builder()
                                .displayName("Gollum")
                                .build())
                        .summary("my precious")
                        .dueDate("2020-12-02")
                        .build())
                .build();
        JsonNode input = DefaultObjectMapper.get().convertValue(jiraIssue, JsonNode.class);
        Object output = normalizationService.normalize(ContentType.fromString("integration/jira/issues"), input);

        System.out.println(output);
        assertThat(output).isInstanceOf(NormalizedJiraIssue.class);
        var normalized = (NormalizedJiraIssue)output;
        assertThat(normalized.getKey()).isEqualTo("LEV-123");
        assertThat(normalized.getAssignee()).isEqualTo("Gollum");
        assertThat(normalized.getTitle()).isEqualTo("my precious");
        assertThat(normalized.getDueAt()).isEqualTo("2020-12-02T00:00:00.000");
    }

}