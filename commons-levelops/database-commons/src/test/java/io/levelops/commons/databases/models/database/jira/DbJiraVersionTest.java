package io.levelops.commons.databases.models.database.jira;

import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class DbJiraVersionTest {

    @Test
    public void testFromJiraProject() throws IOException, ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        JiraProject jiraProject = ResourceUtils.getResourceAsObject("jira/projects/jira_project.json", JiraProject.class);
        DbJiraProject dbJiraProject = DbJiraProject.fromJiraProject(jiraProject, "1");
        List<DbJiraVersion> actual = DbJiraVersion.fromJiraProject(dbJiraProject, "1");
        List<DbJiraVersion> expected = List.of(
                DbJiraVersion.builder()
                        .versionId(12303)
                        .projectId(10368)
                        .name("Test Release")
                        .description("Testing purpose only")
                        .integrationId(1)
                        .archived(false)
                        .released(false)
                        .overdue(true)
                        .startDate(simpleDateFormat.parse("2023-05-02").toInstant())
                        .endDate(simpleDateFormat.parse("2023-05-02").toInstant())
                        .fixVersionUpdatedAt(1698760149L)
                        .build(),
                DbJiraVersion.builder()
                        .versionId(12329)
                        .projectId(10368)
                        .name("202305.1")
                        .integrationId(1)
                        .archived(true)
                        .released(false)
                        .overdue(false)
                        .startDate(simpleDateFormat.parse("2023-05-02").toInstant())
                        .endDate(simpleDateFormat.parse("2023-05-15").toInstant())
                        .fixVersionUpdatedAt(1698760149L)
                        .build()
        );
        Assert.assertEquals(expected, actual);
    }
}
