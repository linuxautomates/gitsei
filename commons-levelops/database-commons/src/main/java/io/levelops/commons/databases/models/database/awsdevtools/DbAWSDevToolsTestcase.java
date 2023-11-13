package io.levelops.commons.databases.models.database.awsdevtools;

import com.amazonaws.services.codebuild.model.TestCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAWSDevToolsTestcase.DbAWSDevToolsTestcaseBuilder.class)
public class DbAWSDevToolsTestcase {

    @JsonProperty("id")
    String id;

    @JsonProperty("report_id")
    String reportId;

    @JsonProperty("report_arn")
    String reportArn;

    @JsonProperty("prefix")
    String prefix;

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("duration")
    Long duration;

    @JsonProperty("expired")
    Date expired;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    public static DbAWSDevToolsTestcase fromTestcase(TestCase testcase, String reportId) {
        Date date = new Date();
        return DbAWSDevToolsTestcase.builder()
                .reportId(reportId)
                .reportArn(testcase.getReportArn())
                .prefix(testcase.getPrefix())
                .name(testcase.getName())
                .status(testcase.getStatus())
                .duration(TimeUnit.SECONDS.convert(testcase.getDurationInNanoSeconds(), TimeUnit.NANOSECONDS))
                .expired(testcase.getExpired())
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
