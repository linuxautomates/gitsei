package io.levelops.commons.databases.models.database.jira.parsers;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueCustomFieldParser;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.database.jira.DbJiraField.CUSTOM_TYPE_RELEASE_LINK_2;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.CUSTOM_TYPE_TEAM;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.CUSTOM_TYPE_TEMPO_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueCustomFieldParserTest {

    private void assertCustomField(String fieldType, Object rawValue, Object expectedParsedValue) {
        String key = "customfield_10100";
        JiraIssueFields fields = JiraIssueFields.builder().build();
        fields.addDynamicField(key, rawValue);
        Object value = JiraIssueCustomFieldParser.parseCustomField(
                JiraIssue.builder().fields(fields).build(),
                List.of(DbJiraField.builder().fieldKey(key).fieldType(fieldType).build()),
                IntegrationConfig.ConfigEntry.builder().key(key).build());
        assertThat(value).isEqualTo(expectedParsedValue);
    }

    @Test
    public void testTeamCustomField() {
        assertCustomField(CUSTOM_TYPE_TEAM, Map.of("title", "Spicy Assets"), "Spicy Assets");
        assertCustomField(CUSTOM_TYPE_TEAM, Map.of("title", "Spicy Assets", "id", "2"), "Spicy Assets");
        assertCustomField(CUSTOM_TYPE_TEAM, Map.of("id", "2"), "2");
        assertCustomField(CUSTOM_TYPE_TEAM, Map.of("id", 2), "2");
        assertCustomField(CUSTOM_TYPE_TEAM, null, null);
        assertCustomField(CUSTOM_TYPE_TEAM, Map.of("test", "test"), null);
    }

    @Test
    public void testReleaseLink2Field() {
        assertCustomField(CUSTOM_TYPE_RELEASE_LINK_2, Map.of("value", "Release 123"), "Release 123");
    }

    @Test
    public void testTempoAccountCustomField() {
        assertCustomField(CUSTOM_TYPE_TEMPO_ACCOUNT, Map.of("value", "Customer Success"), "Customer Success");
    }

}