package io.levelops.commons.databases.models.database.jira;

import io.levelops.integrations.jira.models.JiraField;
import org.junit.Test;

import java.util.List;

import static io.levelops.commons.databases.models.database.jira.DbJiraField.SUPPORTED_CUSTOM_TYPES;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.SUPPORTED_TYPES;
import static org.assertj.core.api.Assertions.assertThat;

public class DbJiraFieldTest {

    @Test
    public void validateFieldsLowercase() {
        // all fields must be lowercase!
        assertThat(SUPPORTED_TYPES).allMatch(str -> str.equals(str.toLowerCase()));
        assertThat(SUPPORTED_CUSTOM_TYPES).allMatch(str -> str.equals(str.toLowerCase()));
    }

    @Test
    public void testCustomFieldKey() {
        assertThat(DbJiraField.isCustomFieldKey("customfield_10286")).isTrue();
        assertThat(DbJiraField.isCustomFieldKey("")).isFalse();
        assertThat(DbJiraField.isCustomFieldKey(null)).isFalse();
        assertThat(DbJiraField.isCustomFieldKey("ajlkasjdklasdasd")).isFalse();
    }

    @Test
    public void testReleaseLink2() {
        DbJiraField jiraField = DbJiraField.fromJiraField(
                JiraField.builder()
                        .id("customfield_10286")
                        .key("net.caelor.jira.cloud.advancedissuelinks__ReleaseLink2-1642081770445")
                        .custom(true)
                        .name("Release Link2")
                        .schema(JiraField.Schema.builder()
                                .type("option2")
                                .custom("com.atlassian.plugins.atlassian-connect-plugin:net.caelor.jira.cloud.advancedissuelinks__ReleaseLink2-1642081770445")
                                .customId(10286)
                                .build())
                        .build(), "1");
        assertThat(jiraField).isNotNull();
        assertThat(jiraField.getIntegrationId()).isEqualTo("1");
        assertThat(jiraField.getName()).isEqualTo("Release Link2");
        assertThat(jiraField.getFieldKey()).isEqualTo("customfield_10286");
        assertThat(jiraField.getFieldType()).isEqualTo("com.atlassian.plugins.atlassian-connect-plugin:net.caelor.jira.cloud.advancedissuelinks__releaselink2-1642081770445");
    }

    @Test
    public void testTempoAccount() {
        DbJiraField jiraField = DbJiraField.fromJiraField(
                JiraField.builder()
                        .id("customfield_10035")
                        .key("io.tempo.jira__account")
                        .custom(true)
                        .name("Account")
                        .schema(JiraField.Schema.builder()
                                .type("option2")
                                .custom("com.atlassian.plugins.atlassian-connect-plugin:io.tempo.jira__account")
                                .customId(10035)
                                .build())
                        .build(), "1");
        assertThat(jiraField).isNotNull();
        assertThat(jiraField.getIntegrationId()).isEqualTo("1");
        assertThat(jiraField.getName()).isEqualTo("Account");
        assertThat(jiraField.getFieldKey()).isEqualTo("customfield_10035");
        assertThat(jiraField.getFieldType()).isEqualTo("com.atlassian.plugins.atlassian-connect-plugin:io.tempo.jira__account");
    }

    @Test
    public void testFabricParentLink() {
        DbJiraField jiraField = DbJiraField.fromJiraField(
                JiraField.builder()
                        .id("customfield_10009")
                        .key("customfield_10009")
                        .name("Parent Link")
                        .orderable(true)
                        .searchable(true)
                        .custom(true)
                        .navigable(true)
                        .clauseNames(List.of("cf[10009]", "Parent Link"))
                        .schema(JiraField.Schema.builder()
                                .type("any")
                                .custom("com.atlassian.jpo:jpo-custom-field-parent")
                                .customId(10009)
                                .build())
                        .build(), "1");
        assertThat(jiraField).isNotNull();
        assertThat(jiraField.getIntegrationId()).isEqualTo("1");
        assertThat(jiraField.getName()).isEqualTo("Parent Link");
        assertThat(jiraField.getFieldKey()).isEqualTo("customfield_10009");
        assertThat(jiraField.getFieldType()).isEqualTo("com.atlassian.jpo:jpo-custom-field-parent");
    }
}