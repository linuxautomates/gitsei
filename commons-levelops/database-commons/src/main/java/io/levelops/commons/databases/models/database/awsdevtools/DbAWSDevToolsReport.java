package io.levelops.commons.databases.models.database.awsdevtools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.awsdevtools.models.CBReport;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAWSDevToolsReport.DbAWSDevToolsReportBuilder.class)
public class DbAWSDevToolsReport {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("arn")
    String arn;

    @JsonProperty("report_type")
    String reportType;

    @JsonProperty("report_group_arn")
    String reportGroupArn;

    @JsonProperty("report_group_name")
    String reportGroupName;

    @JsonProperty("execution_id")
    String executionId;

    @JsonProperty("status")
    String status;

    @JsonProperty("report_created_at")
    Date reportCreatedAt;

    @JsonProperty("report_expired_at")
    Date reportExpiredAt;

    @JsonProperty("duration")
    Long duration;

    @JsonProperty("test_cases")
    List<DbAWSDevToolsTestcase> testcases;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    public static DbAWSDevToolsReport fromReport(CBReport report, String integrationId) {
        Date date = new Date();
        return DbAWSDevToolsReport.builder()
                .integrationId(integrationId)
                .arn(report.getReport().getArn())
                .reportType(report.getReport().getType())
                .reportGroupArn(report.getReport().getReportGroupArn())
                .reportGroupName(report.getReportGroup().getName())
                .executionId(report.getReport().getExecutionId())
                .status(report.getReport().getStatus())
                .reportCreatedAt(report.getReport().getCreated())
                .reportExpiredAt(report.getReport().getExpired())
                .duration(TimeUnit.SECONDS.convert(report.getReport().getTestSummary().getDurationInNanoSeconds(),
                        TimeUnit.NANOSECONDS))
                .testcases(CollectionUtils.emptyIfNull(report.getTestCases()).stream()
                        .map(testCase -> DbAWSDevToolsTestcase.fromTestcase(testCase, builder().id))
                        .collect(Collectors.toList()))
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
