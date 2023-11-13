package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.jenkins.JUnitTestReport;
import io.levelops.integrations.gitlab.models.GitlabTestReport;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.CiCdJobRunTest.Status.FAILED;
import static io.levelops.commons.databases.models.database.CiCdJobRunTest.Status.PASSED;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdJobRunTest.CiCdJobRunTestBuilder.class)
public class CiCdJobRunTest {

    private static final int ERROR_MAX_WIDTH = 1000;

    @JsonProperty("id")
    String id;

    @JsonProperty("cicd_job_run_id")
    String cicdJobRunId;

    @JsonProperty("test_suite")
    String testSuite;

    @JsonProperty("test_name")
    String testName;

    @JsonProperty("status")
    Status status;

    @JsonProperty("duration")
    Float duration;

    @JsonProperty("error_details")
    String errorDetails;

    @JsonProperty("error_stacktrace")
    String errorStackTrace;

    @JsonProperty("job_status")
    String jobStatus;

    @JsonProperty("job_name")
    String jobName;

    @JsonProperty("job_run_number")
    String jobRunNumber;

    @JsonProperty("cicd_user_id")
    String cicdUserId;

    @JsonProperty("start_time")
    Instant startTime;

    @JsonProperty("end_time")
    Instant endTime;

    @JsonProperty("job_normalized_full_name")
    String  jobNormalizedFullName;

    @JsonProperty("project_name")
    String projectName;

    public enum Status {PASSED, FAILED, SKIPPED}

    public static List<CiCdJobRunTest> fromJUnitTestSuite(JUnitTestReport jUnitTestReport, UUID cicdJobRunId) {
        final String name = jUnitTestReport.getName();
        return CollectionUtils.emptyIfNull(jUnitTestReport.getTestCase()).stream()
                .map(testCase -> CiCdJobRunTest.builder()
                        .cicdJobRunId(cicdJobRunId.toString())
                        .testSuite(name)
                        .testName(testCase.getClassName() + "." + testCase.getName())
                        .status(extractStatus(testCase))
                        .duration(testCase.getTime())
                        .errorDetails(Optional.ofNullable(ObjectUtils.firstNonNull(testCase.getFailure(), testCase.getError()))
                                .map(JUnitTestReport.Error::getMessage)
                                .map(message -> StringUtils.truncate(message, ERROR_MAX_WIDTH))
                                .orElse(null))
                        .errorStackTrace(Optional.ofNullable(ObjectUtils.firstNonNull(testCase.getFailure(), testCase.getError()))
                                .map(JUnitTestReport.Error::getFailure)
                                .map(stackTrace -> StringUtils.truncate(stackTrace, ERROR_MAX_WIDTH))
                                .orElse(null))
                        .build())
                .collect(Collectors.toList());
    }

    private static Status extractStatus(JUnitTestReport.TestCase testCase) {
        if (testCase.getSkipped() != null) {
            return Status.SKIPPED;
        } else if ((testCase.getFailure() != null) || (testCase.getError() != null)) {
            return Status.FAILED;
        } else {
            return Status.PASSED;
        }
    }

    public static List<CiCdJobRunTest> fromGitlabTestReport(String cicdJobRunId, GitlabTestReport source) {
        return source.getTestSuites().stream()
                .flatMap(testSuite -> testSuite.getTestCases().stream()
                        .map(gitlabTestCase -> CiCdJobRunTest.builder()
                                .cicdJobRunId(cicdJobRunId)
                                .testSuite(testSuite.getName())
                                .duration(Float.valueOf(gitlabTestCase.getExecutionTime()))
                                .testName(gitlabTestCase.getName())
                                .status("success".equals(gitlabTestCase.getStatus()) ? PASSED : FAILED)
                                .errorStackTrace(gitlabTestCase.getStackTrace())
                                .build()))
                .collect(Collectors.toList());
    }
}
