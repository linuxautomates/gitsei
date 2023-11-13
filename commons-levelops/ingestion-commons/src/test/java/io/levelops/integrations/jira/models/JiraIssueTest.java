package io.levelops.integrations.jira.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueTest {
    String descriptionV3 = "Description!\nCVE-29012\ntest\nadfasd\nasdasdas\nquote\nquoted text\n ";
    String descriptionV2 = "Description\\!\n\nCVE-29012\n\nh1. test\n\nh6. adfasd\n\n{{asdasdas}}\n\n{quote}quote{quote}\n\n{quote}quoted text{quote}\n\n!Screen Shot 2019-09-17 at 2.48.00 PM.png|width=200,height=183!\n\n ";

    @Test
    public void deserializeV3() throws IOException {
        JiraIssue jiraIssue = ResourceUtils.getResourceAsObject("integrations/jira/jira_api_issue_v3.json", JiraIssue.class);

        assertThat(jiraIssue.getFields().getAssignee().getDisplayName()).isEqualTo("Maxime Bellier");
        assertThat(jiraIssue.getFields().getSummary()).isEqualTo("Ingestion - Jira - Fetch Projects");
        assertThat(jiraIssue.getFields().description).isNotInstanceOf(String.class);
        assertThat(jiraIssue.getFields().getDescription()).isNull();
        assertThat(jiraIssue.getFields().getDescriptionText()).isEqualTo(descriptionV3);
        assertThat(jiraIssue.getFields().getDescriptionLength()).isEqualTo(63);

        assertThat(jiraIssue.getFields().getDynamicFields().get("customfield_10019")).isEqualTo("0|i000bz:");
        assertThat(jiraIssue.getFields().getDynamicFields().get("timeestimate")).isEqualTo(28800);
        assertThat(jiraIssue.getFields().getDynamicFields().get("customfield_10000")).isEqualTo("{pullrequest={dataType=pullrequest, state=MERGED, stateCount=2}, json={\"cachedValue\":{\"errors\":[],\"summary\":{\"pullrequest\":{\"overall\":{\"count\":2,\"lastUpdated\":\"2019-09-20T15:44:45.000-0700\",\"stateCount\":2,\"state\":\"MERGED\",\"dataType\":\"pullrequest\",\"open\":false},\"byInstanceType\":{\"GitHub\":{\"count\":2,\"name\":\"GitHub\"}}}}},\"isStale\":true}}");
    }

    @Test
    public void deserializeV2() throws IOException {
        JiraIssue jiraIssue = ResourceUtils.getResourceAsObject("integrations/jira/jira_api_issue_v2.json", JiraIssue.class);

        assertThat(jiraIssue.getFields().getAssignee().getDisplayName()).isEqualTo("Maxime Bellier");
        assertThat(jiraIssue.getFields().getSummary()).isEqualTo("Ingestion - Jira - Fetch Projects");
        assertThat(jiraIssue.getFields().description).isInstanceOf(String.class);
        assertThat(jiraIssue.getFields().getDescription()).isNull();
        assertThat(jiraIssue.getFields().getDescriptionLength()).isEqualTo(176);
        assertThat(jiraIssue.getFields().getDescriptionText()).isEqualTo(descriptionV2);

        assertThat(jiraIssue.getFields().getDynamicFields().get("customfield_10019")).isEqualTo("0|i000bz:");
        assertThat(jiraIssue.getFields().getDynamicFields().get("timeestimate")).isEqualTo(18000);
        assertThat(jiraIssue.getFields().getDynamicFields().get("customfield_10000")).isEqualTo("{}");
    }

    @Test
    public void serial() throws IOException {
        JiraIssue jiraIssue = ResourceUtils.getResourceAsObject("integrations/jira/jira_api_issue_v2.json", JiraIssue.class);
        assertThat(jiraIssue.getFields().description).isNotNull();
        assertThat(jiraIssue.getFields().getDescription()).isNull();

        String json = DefaultObjectMapper.get().writeValueAsString(jiraIssue.getFields());
        JiraIssueFields serialized = DefaultObjectMapper.get().readValue(json, JiraIssueFields.class);

        assertThat(serialized.description).isNull();
        assertThat(serialized.getDescription()).isNull();

    }

}
