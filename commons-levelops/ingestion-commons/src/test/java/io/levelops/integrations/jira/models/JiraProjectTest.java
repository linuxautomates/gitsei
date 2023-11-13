package io.levelops.integrations.jira.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraProjectTest {

    @Test
    public void deserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("integrations/jira/jira_api_project.json");
        JiraProject output = DefaultObjectMapper.get().readValue(input, JiraProject.class);
        assertThat(output.getId()).isEqualTo("10001");
        assertThat(output.getDescription()).isEqualTo("ça va bien?");
        assertThat(output.getLead()).isNotNull();
        assertThat(output.getLead().getName()).isEqualTo("nishant");
        assertThat(output.getLead().getDisplayName()).isEqualTo("Nishant Doshi");
        assertThat(output.getIssueTypes()).hasSize(5);
    }

}