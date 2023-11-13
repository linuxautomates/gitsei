package io.levelops.integrations.awsdevtools.client;

import com.amazonaws.services.codebuild.AWSCodeBuild;
import com.amazonaws.services.codebuild.model.*;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AWSDevTools Client class which should be used for making calls to AWSDevTools.
 */
@Log4j2
public class AWSDevToolsClient {

    private final AWSCodeBuild codeBuild;
    private final String region;
    private final int pageSize;

    /**
     * all arg constructor for {@link AWSDevToolsClient} class
     *
     * @param codeBuild {@link AWSCodeBuild} object to be used for making calls to code build
     * @param region    aws region for the code build client
     * @param pageSize  response page size
     */
    @Builder
    public AWSDevToolsClient(AWSCodeBuild codeBuild, String region, Integer pageSize) {
        this.codeBuild = codeBuild;
        this.region = region;
        this.pageSize = pageSize != 0 ? pageSize : AWSDevToolsClientFactory.DEFAULT_PAGE_SIZE;
    }

    /**
     * Fetches the names of all {@link Project}
     *
     * @return {@link ListProjectsResult} containing the projects
     */
    public ListProjectsResult listProjects(String token) throws AWSDevToolsClientException {
        ListProjectsRequest listProjectsRequest = new ListProjectsRequest();
        listProjectsRequest.setSortOrder(SortOrderType.DESCENDING);
        listProjectsRequest.setSortBy(ProjectSortByType.LAST_MODIFIED_TIME);
        if (token != null) {
            listProjectsRequest.setNextToken(token);
        }
        ListProjectsResult listProjectsResult;
        try {
            listProjectsResult = codeBuild.listProjects(listProjectsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to fetch the list of project names", e);
        }
        return listProjectsResult;
    }

    /**
     * Fetches the list of all {@link Project}
     *
     * @return {@link List<Project>} containing the details of the projects
     */
    public List<Project> getProjects(AWSDevToolsQuery query, List<String> projectNames) throws AWSDevToolsClientException {
        if (CollectionUtils.isEmpty(projectNames))
            return Collections.emptyList();
        BatchGetProjectsRequest batchGetProjectsRequest = new BatchGetProjectsRequest();
        batchGetProjectsRequest.setNames(projectNames);
        BatchGetProjectsResult batchGetProjectsResult;
        try {
            batchGetProjectsResult = codeBuild.batchGetProjects(batchGetProjectsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get projects", e);
        }
        if (query.getFrom() != null) {
            return batchGetProjectsResult.getProjects()
                    .stream()
                    .takeWhile(project -> TimeUnit.MILLISECONDS.toSeconds(project.getLastModified().getTime()) >=
                            TimeUnit.MILLISECONDS.toSeconds(query.getFrom().getTime()) &&
                            TimeUnit.MILLISECONDS.toSeconds(project.getLastModified().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        } else {
            return batchGetProjectsResult.getProjects()
                    .stream()
                    .takeWhile(project -> TimeUnit.MILLISECONDS.toSeconds(project.getLastModified().getTime()) >=
                            Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond() &&
                            TimeUnit.MILLISECONDS.toSeconds(project.getLastModified().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        }
    }

    public List<Project> getProjects(List<String> projectNames) throws AWSDevToolsClientException {
        if (CollectionUtils.isEmpty(projectNames))
            return Collections.emptyList();
        BatchGetProjectsRequest batchGetProjectsRequest = new BatchGetProjectsRequest();
        batchGetProjectsRequest.setNames(projectNames);
        codeBuild.batchGetProjects(batchGetProjectsRequest);
        BatchGetProjectsResult batchGetProjectsResult;
        try {
            batchGetProjectsResult = codeBuild.batchGetProjects(batchGetProjectsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get projects", e);
        }
        return batchGetProjectsResult.getProjects();
    }

    /**
     * Fetches the names of all {@link Build}
     *
     * @return {@link ListBuildsResult} containing the builds
     */
    public ListBuildsResult listBuilds(String token) throws AWSDevToolsClientException {
        ListBuildsRequest listBuildsRequest = new ListBuildsRequest();
        listBuildsRequest.setSortOrder(SortOrderType.DESCENDING);
        if (token != null) {
            listBuildsRequest.setNextToken(token);
        }
        ListBuildsResult listBuildsResult;
        try {
            listBuildsResult = codeBuild.listBuilds(listBuildsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to fetch the list of build Ids", e);
        }
        return listBuildsResult;
    }

    /**
     * Fetches the list of all {@link Build}
     *
     * @return {@link List<Build>} containing the details of the builds
     */
    public List<Build> getBuilds(AWSDevToolsQuery query, List<String> buildIds) throws AWSDevToolsClientException {
        if (CollectionUtils.isEmpty(buildIds))
            return Collections.emptyList();
        BatchGetBuildsRequest batchGetBuildsRequest = new BatchGetBuildsRequest();
        batchGetBuildsRequest.setIds(buildIds);
        BatchGetBuildsResult batchGetBuildsResult;
        try {
            batchGetBuildsResult = codeBuild.batchGetBuilds(batchGetBuildsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get builds", e);
        }
        if (query.getFrom() != null) {
            return batchGetBuildsResult.getBuilds()
                    .stream()
                    .takeWhile(build -> TimeUnit.MILLISECONDS.toSeconds(build.getStartTime().getTime()) >=
                            TimeUnit.MILLISECONDS.toSeconds(query.getFrom().getTime()) &&
                            TimeUnit.MILLISECONDS.toSeconds(build.getStartTime().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        } else {
            return batchGetBuildsResult.getBuilds()
                    .stream()
                    .takeWhile(build -> TimeUnit.MILLISECONDS.toSeconds(build.getStartTime().getTime()) >=
                            Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond() &&
                            TimeUnit.MILLISECONDS.toSeconds(build.getStartTime().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Fetches the names of all {@link BuildBatch}
     *
     * @return {@link ListBuildBatchesResult} containing the build batches
     */
    public ListBuildBatchesResult listBuildBatches(String token) throws AWSDevToolsClientException {
        ListBuildBatchesRequest listBuildBatchesRequest = new ListBuildBatchesRequest();
        listBuildBatchesRequest.setMaxResults(pageSize);
        listBuildBatchesRequest.setSortOrder(String.valueOf(SortOrderType.DESCENDING));
        if (token != null) {
            listBuildBatchesRequest.setNextToken(token);
        }
        ListBuildBatchesResult listBuildBatchesResult;
        try {
            listBuildBatchesResult = codeBuild.listBuildBatches(listBuildBatchesRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to fetch the list of build batch Ids", e);
        }
        return listBuildBatchesResult;
    }

    /**
     * Fetches the list of all {@link BuildBatch}
     *
     * @return {@link List<BuildBatch>} containing the details of the build batches
     */
    public List<BuildBatch> getBuildBatches(AWSDevToolsQuery query, List<String> buildBatchesIds) throws AWSDevToolsClientException {
        if (CollectionUtils.isEmpty(buildBatchesIds))
            return Collections.emptyList();
        BatchGetBuildBatchesRequest batchGetBuildBatchesRequest = new BatchGetBuildBatchesRequest();
        batchGetBuildBatchesRequest.setIds(buildBatchesIds);
        BatchGetBuildBatchesResult batchGetBuildBatchesResult;
        try {
            batchGetBuildBatchesResult = codeBuild.batchGetBuildBatches(batchGetBuildBatchesRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get build batches", e);
        }
        if (query.getFrom() != null) {
            return batchGetBuildBatchesResult.getBuildBatches()
                    .stream()
                    .takeWhile(buildBatch -> TimeUnit.MILLISECONDS.toSeconds(buildBatch.getStartTime().getTime()) >=
                            TimeUnit.MILLISECONDS.toSeconds(query.getFrom().getTime()) &&
                            TimeUnit.MILLISECONDS.toSeconds(buildBatch.getStartTime().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        } else {
            return batchGetBuildBatchesResult.getBuildBatches()
                    .stream()
                    .takeWhile(buildBatch -> TimeUnit.MILLISECONDS.toSeconds(buildBatch.getStartTime().getTime()) >=
                            Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond() &&
                            TimeUnit.MILLISECONDS.toSeconds(buildBatch.getStartTime().getTime()) <=
                                    TimeUnit.MILLISECONDS.toSeconds(query.getTo().getTime()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Fetches the names of all {@link Report}
     *
     * @return {@link ListReportsResult} containing the reports
     */
    public ListReportsResult listReports(String token) throws AWSDevToolsClientException {
        ListReportsRequest listReportsRequest = new ListReportsRequest();
        listReportsRequest.setMaxResults(pageSize);
        if (token != null) {
            listReportsRequest.setNextToken(token);
        }
        ListReportsResult listReportsResult;
        try {
            listReportsResult = codeBuild.listReports(listReportsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to fetch the list of report Arns", e);
        }
        return listReportsResult;
    }

    /**
     * Fetches the list of all {@link Report}
     *
     * @return {@link List<Report>} containing the details of the reports
     */
    public List<Report> getReports(List<String> reportArns) throws AWSDevToolsClientException {
        if (CollectionUtils.isNotEmpty(reportArns))
            return Collections.emptyList();
        BatchGetReportsRequest batchGetReportsRequest = new BatchGetReportsRequest();
        batchGetReportsRequest.setReportArns(reportArns);
        BatchGetReportsResult batchGetReportsResult;
        try {
            batchGetReportsResult = codeBuild.batchGetReports(batchGetReportsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get reports", e);
        }
        return batchGetReportsResult.getReports();
    }

    /**
     * Fetches the names of all {@link ReportGroup}
     *
     * @return {@link ListProjectsResult} containing the report groups
     */
    public ListReportGroupsResult listReportGroups(String token) throws AWSDevToolsClientException {
        ListReportGroupsRequest listReportGroupsRequest = new ListReportGroupsRequest();
        listReportGroupsRequest.setMaxResults(pageSize);
        if (token != null) {
            listReportGroupsRequest.setNextToken(token);
        }
        ListReportGroupsResult listReportGroupsResult;
        try {
            listReportGroupsResult = codeBuild.listReportGroups(listReportGroupsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to fetch the list of report group Arns", e);
        }
        return listReportGroupsResult;
    }

    /**
     * Fetches the list of all {@link ReportGroup}
     *
     * @return {@link List<ReportGroup>} containing the details of the report groups
     */
    public List<ReportGroup> getReportGroups(List<String> reportGroupArns) throws AWSDevToolsClientException {
        if (CollectionUtils.isEmpty(reportGroupArns))
            return Collections.emptyList();
        BatchGetReportGroupsRequest batchGetReportGroupsRequest = new BatchGetReportGroupsRequest();
        batchGetReportGroupsRequest.setReportGroupArns(reportGroupArns);
        BatchGetReportGroupsResult batchGetReportGroupsResult;
        try {
            batchGetReportGroupsResult = codeBuild.batchGetReportGroups(batchGetReportGroupsRequest);
        } catch (Exception e) {
            throw new AWSDevToolsClientException("Failed to get report groups", e);
        }
        return batchGetReportGroupsResult.getReportGroups();
    }

    /**
     * Fetches the list of all {@link TestCase}
     *
     * @return {@link List<TestCase>} containing the details of the test cases
     */
    public List<TestCase> getTestCase(String reportArn) throws AWSDevToolsClientException {
        String token = StringUtils.EMPTY;
        List<TestCase> testCases = new ArrayList<>();
        DescribeTestCasesRequest testCasesRequest = new DescribeTestCasesRequest();
        testCasesRequest.setReportArn(reportArn);
        do {
            testCasesRequest.setNextToken(token);
            DescribeTestCasesResult testCasesResult;
            try {
                testCasesResult = codeBuild.describeTestCases(testCasesRequest);
            } catch (Exception e) {
                throw new AWSDevToolsClientException("Failed to get test cases", e);
            }
            testCases.addAll(testCasesResult.getTestCases());
            token = testCasesResult.getNextToken();
        }
        while (token != null);
        return testCases;
    }

    public String getRegion() {
        return region;
    }
}
