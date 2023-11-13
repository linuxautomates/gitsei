package io.levelops.integrations.jira.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraUserTest {

    @Test
    public void deserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("integrations/jira/jira_api_user.json");
        JiraUser jiraApiUser = DefaultObjectMapper.get().readValue(input, JiraUser.class);
        assertThat(jiraApiUser.getAccountId()).isEqualTo("5d6049c45a68ef0ca625940b");
        assertThat(jiraApiUser.getDisplayName()).isEqualTo("Nishant Doshi");
        assertThat(jiraApiUser.getActive()).isEqualTo(true);
        assertThat(jiraApiUser.getName()).isEqualTo("nishant");
    }


    @Test
    public void deserializeNoAccountId() throws IOException {
        String input = ResourceUtils.getResourceAsString("integrations/jira/jira_api_user_no_account_id.json");
        JiraUser jiraApiUser = DefaultObjectMapper.get().readValue(input, JiraUser.class);
        assertThat(jiraApiUser.getAccountId()).isEqualTo("5d6049c45a68ef0ca625940b");
        assertThat(jiraApiUser.getDisplayName()).isEqualTo("Nishant Doshi");
        assertThat(jiraApiUser.getActive()).isEqualTo(true);
        assertThat(jiraApiUser.getName()).isEqualTo("nishant");
    }

}
