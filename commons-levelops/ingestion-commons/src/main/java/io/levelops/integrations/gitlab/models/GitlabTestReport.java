package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabTestReport.GitlabTestReportBuilder.class)
public class GitlabTestReport {

    @JsonProperty("total_time")
    String totalTime;
    @JsonProperty("total_count")
    String totalCount;
    @JsonProperty("success_count")
    String successCount;
    @JsonProperty("failed_count")
    String failedCount;
    @JsonProperty("skipped_count")
    String skippedCount;
    @JsonProperty("error_count")
    String errorCount;
    @JsonProperty("test_suites")
    List<GitlabTestSuite> testSuites;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabTestSuite.GitlabTestSuiteBuilder.class)
    public static class GitlabTestSuite {

        @JsonProperty("name")
        String name;
        @JsonProperty("total_time")
        String totalTime;
        @JsonProperty("total_count")
        String totalCount;
        @JsonProperty("success_count")
        String successCount;
        @JsonProperty("failed_count")
        String failedCount;
        @JsonProperty("skipped_count")
        String skippedCount;
        @JsonProperty("error_count")
        String errorCount;
        @JsonProperty("test_cases")
        List<GitlabTestCase> testCases;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = GitlabTestCase.GitlabTestCaseBuilder.class)
        public static class GitlabTestCase {

            @JsonProperty("status")
            String status;
            @JsonProperty("name")
            String name;
            @JsonProperty("classname")
            String className;
            @JsonProperty("execution_time")
            String executionTime;
            @JsonProperty("system_output")
            String systemOutput;
            @JsonProperty("stack_trace")
            String stackTrace;
        }
    }
}
