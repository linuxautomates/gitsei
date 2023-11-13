package io.levelops.integrations.jira.client;

import com.google.common.base.MoreObjects;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.jira.models.JiraCreateIssueFields;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JiraInternalClientFactoryIntegrationTest {
    private JiraInternalClientFactory clientFactory;

    @Before
    public void setUp() {
        OkHttpClient okHttpClient = new OkHttpClient();
        var token = MoreObjects.firstNonNull(System.getenv("JIRA_TOKEN") , "token");
        var userName = MoreObjects.firstNonNull(System.getenv("JIRA_USERNAME") , "username");

        clientFactory = new JiraInternalClientFactory(
                DefaultObjectMapper.get(), okHttpClient, 3.0, token, userName);
    }
    @Test
    public void testCreateTicket() throws JiraClientException {
        JiraClient jiraClient = clientFactory.get();
        var issue = jiraClient.createIssue(JiraCreateIssueFields.builder()
                .assignee(JiraUser.builder()
                        .accountId("5d6049c4c812c40d27c01ea9")
                        .build())
                .summary("Create tenant for sid-tenant")
                .description("please ignore - sid is testing")
                .project(JiraProject.builder()
                        .id("10020") // 10020 ITOPS
                        .build())
                .issueType(JiraIssueType.builder()
                        .id(10002L)
                        .build())
                .labels(List.of("tenantCreation"))
                        .priority(JiraIssueFields.JiraPriority.builder().name("High").build())
                .build());
        var j = jiraClient.getMyself();
        var p = jiraClient.getProjects();

        assertThat(issue).isNotNull();
        assertThat(j).isNotNull();
        assertThat(p.size()).isNotZero();
    }
}

