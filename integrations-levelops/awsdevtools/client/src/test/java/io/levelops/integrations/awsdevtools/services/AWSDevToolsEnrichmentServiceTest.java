package io.levelops.integrations.awsdevtools.services;

import com.amazonaws.services.codebuild.model.*;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import io.levelops.integrations.awsdevtools.models.CBReport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AWSDevToolsEnrichmentServiceTest {

    private AWSDevToolsClient awsDevToolsClient;
    private AWSDevToolsEnrichmentService enrichmentService;

    @Before
    public void setup() throws AWSDevToolsClientException {
        awsDevToolsClient = Mockito.mock(AWSDevToolsClient.class);
        enrichmentService = new AWSDevToolsEnrichmentService(2, 10);

        Project project = new Project();
        project.setName("Demo-project");
        project.setArn("arn:aws:codebuild:us-east-2:project-demo");
        List<Project> projects = List.of(project);
        when(awsDevToolsClient.getProjects(List.of("Demo-project")))
                .thenReturn(projects);

        Report report = new Report();
        report.setArn("arn:aws:codebuild:us-east-2:report-demo");
        report.setReportGroupArn("arn:aws:codebuild:us-east-2:report-group-demo");
        List<Report> reports = List.of(report);
        when(awsDevToolsClient.getReports(List.of("arn:aws:codebuild:us-east-2:report-demo")))
                .thenReturn(reports);

        ReportGroup reportGroup = new ReportGroup();
        reportGroup.setArn("arn:aws:codebuild:us-east-2:report-group-demo");
        List<ReportGroup> reportGroups = List.of(reportGroup);
        when(awsDevToolsClient.getReportGroups(List.of("arn:aws:codebuild:us-east-2:report-group-demo")))
                .thenReturn(reportGroups);

        TestCase testCase = new TestCase();
        testCase.setReportArn("arn:aws:codebuild:us-east-2:report-demo");
        when(awsDevToolsClient.getTestCase(anyString()))
                .thenReturn(List.of(testCase));
    }

    @Test
    public void enrichBuild() {
        Build build = new Build();
        build.setId("codebuild-build-demo");
        build.setProjectName("Demo-project");
        build.setReportArns(List.of("arn:aws:codebuild:us-east-2:report-demo"));
        List<CBBuild> cbBuilds = enrichmentService.enrichBuilds(awsDevToolsClient,
                IntegrationKey.builder().build(), "US_EAST_2", List.of(build));
        assertThat(cbBuilds).isNotNull();
        assertThat(cbBuilds).hasSize(1);
        CBBuild cbBuild = cbBuilds.get(0);
        List<CBReport> reports = cbBuild.getReports();
        assertThat(reports).isNotNull();
        assertThat(reports).hasSize(1);
        CBReport cbReport = reports.get(0);
        assertThat(cbReport.getReportGroup()).isNotNull();
        List<TestCase> testCases = cbReport.getTestCases();
        assertThat(testCases).isNotNull();
        assertThat(testCases).hasSize(1);
        assertThat(cbBuild.getProjectArn()).isEqualTo("arn:aws:codebuild:us-east-2:project-demo");
        assertThat(cbBuild.getRegion()).isEqualTo("US_EAST_2");
    }

    @Test
    public void enrichBuildBatch() {
        BuildBatch buildBatch = new BuildBatch();
        buildBatch.setId("codebuild-build-batch-demo");
        buildBatch.setProjectName("Demo-project");
        List<CBBuildBatch> cbBuildBatches = enrichmentService.enrichBuildBatches(awsDevToolsClient,
                IntegrationKey.builder().build(), "US_EAST_2", List.of(buildBatch));
        assertThat(cbBuildBatches).isNotNull();
        assertThat(cbBuildBatches).hasSize(1);
        CBBuildBatch cbBuildBatch = cbBuildBatches.get(0);
        assertThat(cbBuildBatch.getProjectArn()).isEqualTo("arn:aws:codebuild:us-east-2:project-demo");
        assertThat(cbBuildBatch.getRegion()).isEqualTo("US_EAST_2");
    }
}
