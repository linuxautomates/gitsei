package io.levelops.commons.databases.models.database.issue_management;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.Project;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class DbIssueManagementModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final Project PROJECT = Project.builder().id("71737302-3511-4626-a89f-585fe0674cef").name("project-test-4").build();


    @Test
    public void testFromAzureDevOpsIteration() throws IOException {
        String data = ResourceUtils.getResourceAsString("issue_management/iteration.json");
        Iteration iteration = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Iteration.class));

        DbIssuesMilestone dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertEquals("71737302-3511-4626-a89f-585fe0674cef", dbIssuesMilestone.getProjectId());
        Assert.assertEquals("project-test-4\\Sprint 0", dbIssuesMilestone.getParentFieldValue());
        Assert.assertEquals("Sprint 1", dbIssuesMilestone.getName());
        Assert.assertEquals(NumberUtils.toInteger("1"), dbIssuesMilestone.getIntegrationId());

        iteration = iteration.toBuilder()
                .path("project-test-4\\Sprint 1")
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertEquals("project-test-4", dbIssuesMilestone.getParentFieldValue());

        iteration = iteration.toBuilder()
                .path("project-test-4\\Sprint 0\\Sprint 1\\Sprint 2")
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertEquals("project-test-4\\Sprint 0\\Sprint 1", dbIssuesMilestone.getParentFieldValue());

        iteration = iteration.toBuilder()
                .attributes(Iteration.Attributes.builder()
                        .startDate("2007-12-03T10:15:30.01Z")
                        .finishDate("2007-12-03T10:15:30.012Z")
                        .build())
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertEquals(Long.parseLong("1196676930010"), dbIssuesMilestone.getStartDate().getTime());
        Assert.assertEquals(Long.parseLong("1196676930012"), dbIssuesMilestone.getEndDate().getTime());

        iteration = iteration.toBuilder()
                .attributes(Iteration.Attributes.builder()
                        .startDate("2007-12-03T10:15:30.0123Z")
                        .finishDate("2007-12-03T10:15:30.01234Z")
                        .build())
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertEquals(Long.parseLong("1196676930012"), dbIssuesMilestone.getStartDate().getTime());
        Assert.assertEquals(Long.parseLong("1196676930012"), dbIssuesMilestone.getEndDate().getTime());

        iteration = iteration.toBuilder()
                .attributes(Iteration.Attributes.builder()
                        .startDate(null)
                        .finishDate(null)
                        .timeFrame(null)
                        .build())
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertNull(dbIssuesMilestone.getStartDate());
        Assert.assertNull(dbIssuesMilestone.getEndDate());
        Assert.assertNull(dbIssuesMilestone.getState());

        iteration = iteration.toBuilder()
                .attributes(null)
                .build();
        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration(
                "1", PROJECT, iteration);
        Assert.assertNull(dbIssuesMilestone.getStartDate());
        Assert.assertNull(dbIssuesMilestone.getEndDate());
        Assert.assertNull(dbIssuesMilestone.getState());
    }

    @Test
    public void testClosedAfterEndDate() {
        Iteration iteration = Iteration.builder()
                .attributes(Iteration.Attributes.builder()
                        .startDate("2007-12-03T10:15:30.0123Z")
                        .finishDate("2007-12-04T10:15:30.0123Z")
                        .timeFrame("current")
                        .build())
                .build();

        DbIssuesMilestone dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration("1", PROJECT, iteration);
        assertThat(dbIssuesMilestone.getStartDate().toInstant()).isEqualTo(Instant.parse("2007-12-03T10:15:30.0123Z"));
        assertThat(dbIssuesMilestone.getEndDate().toInstant()).isEqualTo(Instant.parse("2007-12-04T10:15:30.0123Z"));
        assertThat(dbIssuesMilestone.getState()).isEqualTo("current");

        dbIssuesMilestone = DbIssuesMilestone.fromAzureDevOpsIteration("1", PROJECT, iteration, Instant.parse("2007-12-05T10:15:30.0123Z"), true);
        assertThat(dbIssuesMilestone.getState()).isEqualTo("past");
    }
}
