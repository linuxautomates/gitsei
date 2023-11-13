package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static io.levelops.commons.databases.models.database.jira.DbJiraSprint.SPRINT_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

public class DbJiraSprintTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testParseStringSprint() {
        var result = DbJiraSprint.parseStringSprintFields(
                "id=220344,rapidViewId=87230,state=ACTIVE,name=APA-22.3.4 (9/21- 10/04)" +
                        ",startDate=2022-09-21T09:47:00.000Z," +
                        "endDate=2022-10-04T23:47:00.000Z,completeDate=<null>," +
                        "activatedDate=2022-09-21T13:00:51.407Z,sequence=220344," +
                        "goal=Reshmi = 5 days,autoStartStop=false");
        assertThat(result.size()).isEqualTo(11);
        assertThat(result.get(9).getKey()).isEqualTo("goal");
        assertThat(result.get(9).getValue()).isEqualTo("Reshmi = 5 days");

        result = DbJiraSprint.parseStringSprintFields("id=123,goal=hi,this is sid,start=today");
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(1).getKey()).isEqualTo("goal");
        assertThat(result.get(1).getValue()).isEqualTo("hi,this is sid");

        result = DbJiraSprint.parseStringSprintFields("id=123");
        assertThat(result.size()).isEqualTo(1);

        result = DbJiraSprint.parseStringSprintFields("");
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testSprintAsJsonArray() throws IOException {
        String data = ResourceUtils.getResourceAsString("samples/database/jira_issues_with_sprints.json");
        Assert.assertNotNull(data);
        List<JiraIssue> jiraIssues = MAPPER.readValue(data, MAPPER.getTypeFactory()
                .constructCollectionType(List.class, JiraIssue.class));
        Set<DbJiraSprint> sprints = new HashSet<>();
        for (JiraIssue jiraIssue : jiraIssues) {
            List<DbJiraSprint> dbJiraSprints = DbJiraSprint.fromJiraIssue(jiraIssue,
                    "1",
                    "customfield_10020");
            sprints.addAll(dbJiraSprints);
        }
        assertThat(sprints.size()).isEqualTo(6);
    }

    @Test
    public void testSprintAsStringArray() throws IOException {
        String data = ResourceUtils.getResourceAsString("samples/database/jira_issues_with_sprints_2.json");
        Assert.assertNotNull(data);
        List<JiraIssue> jiraIssues = MAPPER.readValue(data, MAPPER.getTypeFactory()
                .constructCollectionType(List.class, JiraIssue.class));
        Set<DbJiraSprint> sprints = new HashSet<>();
        for (JiraIssue jiraIssue : jiraIssues) {
            List<DbJiraSprint> dbJiraSprints = DbJiraSprint.fromJiraIssue(jiraIssue,
                    "1",
                    "customfield_10020");
            sprints.addAll(dbJiraSprints);
        }
        assertThat(sprints.size()).isEqualTo(5);
        data = ResourceUtils.getResourceAsString("samples/database/jira_issues_with_sprints_date_formats.json");
        Assert.assertNotNull(data);
        jiraIssues = MAPPER.readValue(data, MAPPER.getTypeFactory()
                .constructCollectionType(List.class, JiraIssue.class));
        Set<DbJiraSprint> sprintsWithDifferentDateTypes = new HashSet<>();
        for (JiraIssue jiraIssue : jiraIssues) {
            List<DbJiraSprint> dbJiraSprints = DbJiraSprint.fromJiraIssue(jiraIssue,
                    "1", "customfield_10020");
            sprintsWithDifferentDateTypes.addAll(dbJiraSprints);
        }
        assertThat(sprintsWithDifferentDateTypes.size()).isEqualTo(4);
        assertThat(sprintsWithDifferentDateTypes.stream().filter(c -> c.getStartDate() == null).findFirst()).isEmpty();
        assertThat(sprintsWithDifferentDateTypes.stream().filter(c -> c.getEndDate() == null).findFirst()).isEmpty();
    }

    @Test
    public void testActivatedDate() {
        Date currentTime = new Date();
        JiraIssueFields fields = JiraIssueFields.builder().updated(currentTime).build();
        fields.addDynamicField("customfield_10020", List.of("\"com.atlassian.greenhopper.service.sprint.Sprint@70a1b904[id=42,rapidViewId=108,state=CLOSED,name=Test 04-20-2021,startDate=2021-04-06T10:16:00.000-07:00,endDate=2021-04-20T10:16:00.000-07:00,completeDate=2021-04-20T10:02:38.759-07:00,activatedDate=2021-04-06T10:16:43.147-07:00,sequence=4861,goal=,autoStartStop=false]"));

        List<DbJiraSprint> sprints = DbJiraSprint.fromJiraIssue(JiraIssue.builder().fields(fields).build(), "1", "customfield_10020");
        DefaultObjectMapper.prettyPrint(sprints);
        assertThat(sprints).hasSize(1);
        assertThat(sprints.get(0)).isEqualTo(DbJiraSprint.builder()
                .sprintId(42)
                .name("Test 04-20-2021")
                .integrationId(1)
                .state("CLOSED")
                .startDate(1617729403L) // Tuesday, April 6, 2021 10:16:43 GMT-07:00 DST
                .endDate(1618938960L)
                .completedDate(1618938158L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
    }

    @Test
    public void testRegex() {
        String data = "com.atlassian.greenhopper.service.sprint.Sprint@b8f96dd[id=30956,rapidViewId=4559,state=FUTURE,name=FY22.PI2.Vehicle.S1,startDate=<null>,endDate=<null>,completeDate=<null>,sequence=30956,goal=Scoping for DMS Vehicle's work for PI 2, finalising features for PI 2, Refinement of stories for 2 sprints.]";
        Matcher m = SPRINT_PATTERN.matcher(data);
        Assert.assertTrue(m.find());
        Assert.assertEquals(m.group("VALUE"), "id=30956,rapidViewId=4559,state=FUTURE,name=FY22.PI2.Vehicle.S1,startDate=<null>,endDate=<null>,completeDate=<null>,sequence=30956,goal=Scoping for DMS Vehicle's work for PI 2, finalising features for PI 2, Refinement of stories for 2 sprints.");
    }

    @Test
    public void testSpringGoalWithCommas() {
        Date currentTime = new Date();
        JiraIssueFields fields = JiraIssueFields.builder().updated(currentTime).build();
        fields.addDynamicField("customfield_10020", List.of("com.atlassian.greenhopper.service.sprint.Sprint@b8f96dd[id=30956,rapidViewId=4559,state=FUTURE,name=FY22.PI2.Vehicle.S1,startDate=<null>,endDate=<null>,completeDate=<null>,sequence=30956,goal=Scoping for DMS Vehicle's work for PI 2, finalising features for PI 2, Refinement of stories for 2 sprints.]"));

        List<DbJiraSprint> sprints = DbJiraSprint.fromJiraIssue(JiraIssue.builder().fields(fields).build(), "1", "customfield_10020");
        DefaultObjectMapper.prettyPrint(sprints);
        assertThat(sprints).hasSize(1);
        assertThat(sprints.get(0)).isEqualTo(DbJiraSprint.builder()
                .sprintId(30956)
                .name("FY22.PI2.Vehicle.S1")
                .integrationId(1)
                .state("FUTURE")
                .goal("Scoping for DMS Vehicle's work for PI 2, finalising features for PI 2, Refinement of stories for 2 sprints.")
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
    }
}
