package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations.services.CustomFieldService;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@Log4j2
public class AzureDevopsAggHelperTest {

    private AzureDevopsAggHelper helper;

    @Mock
    DataSource dataSource;
    @Mock
    JobDtoParser jobDtoParser;
    @Mock
    ScmAggService scmAggService;
    @Mock
    AzureDevopsProjectService azureDevopsProjectService;
    @Mock
    AzureDevopsReleaseService azureDevopsReleaseService;
    @Mock
    IntegrationTrackingService integrationTrackingService;
    @Mock
    WorkItemsService workItemsService;
    @Mock
    IssueMgmtProjectService issueMgmtProjectService;
    @Mock
    IssuesMilestoneService issuesMilestoneService;
    @Mock
    WorkItemTimelineService workItemTimelineService;
    @Mock
    WorkItemsMetadataService workItemsMetadataService;
    @Mock
    CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    @Mock
    IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    @Mock
    WorkItemFieldsMetaService workItemFieldsMetaService;

    @Mock
    CustomFieldService customFieldService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        helper = new AzureDevopsAggHelper(dataSource, jobDtoParser, scmAggService, azureDevopsProjectService, azureDevopsReleaseService,
                integrationTrackingService, workItemsService, issueMgmtProjectService, issuesMilestoneService,
                workItemTimelineService, workItemsMetadataService, ciCdInstancesDatabaseService,
                issueMgmtSprintMappingDatabaseService, workItemFieldsMetaService, customFieldService);
    }

    @Test
    public void test() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("azuredevops/azure_devops_assignees.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<DbWorkItemHistory> assignees = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, DbWorkItemHistory.class));
        DbWorkItem dbWorkItem1 = DbWorkItem.builder()
                .workItemId("119")
                .build();
        DbWorkItem hopsAndBounces = helper.computeHopsAndBounces(dbWorkItem1, assignees);
        assertThat(hopsAndBounces.getHops()).isEqualTo(7);
        assertThat(hopsAndBounces.getBounces()).isEqualTo(4);
    }

    @Test
    public void parseIterationPath() {
        assertThat(AzureDevopsAggHelper.parseIterationPath("Sprint 18"))
                .isEqualTo(Pair.of(null, "Sprint 18"));
        assertThat(AzureDevopsAggHelper.parseIterationPath("Parent Level\\Sprint 18"))
                .isEqualTo(Pair.of("Parent Level", "Sprint 18"));
        assertThat(AzureDevopsAggHelper.parseIterationPath("Top Level\\Parent\\Sprint 18"))
                .isEqualTo(Pair.of("Top Level\\Parent", "Sprint 18"));

    }
}
