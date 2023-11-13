package io.levelops.integrations.jira.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraFieldTest {

    @Test
    public void deserial() throws IOException {
        String input = ResourceUtils.getResourceAsStringOrThrow("integrations/jira/jira_api_field_custom.json");
        JiraField jiraField = DefaultObjectMapper.get().readValue(input, JiraField.class);
        assertThat(jiraField.getId()).isEqualTo("customfield_10010");
        assertThat(jiraField.getName()).isEqualTo("Request Type");
        assertThat(jiraField.getSchema().getCustom()).isEqualTo("com.atlassian.servicedesk:vp-origin");
    }
}
