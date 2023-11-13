package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.Test;
import io.levelops.integrations.testrails.models.User;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsTestResult.DbTestRailsTestResultBuilder.class)
public class DbTestRailsTestResult {
    private static final String UNASSIGNED = "_UNASSIGNED_";
    @JsonProperty("id")
    String id;
    @JsonProperty("result_id")
    Long resultId;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("test_id")
    String testId;
    @JsonProperty("status")
    String status;
    @JsonProperty("created_on")
    Instant createdOn;
    @JsonProperty("assignee")
    String assignee;
    @JsonProperty("comment")
    String comment;
    @JsonProperty("elapsed")
    String elapsed;
    @JsonProperty("defects")
    List<String> defects;
    @JsonProperty("creator")
    String creator;
    @JsonProperty("ingested_at")
    Date ingestedAt;

    public static DbTestRailsTestResult fromTestResult(Test.Result testResult, String integrationId) {
        return DbTestRailsTestResult.builder()
                .integrationId(integrationId)
                .resultId(testResult.getId())
                .assignee(Optional.ofNullable(testResult.getAssignee()).map(User::getEmail).orElse(UNASSIGNED))
                .creator(Optional.ofNullable(testResult.getCreator()).map(User::getEmail).orElse(UNASSIGNED))
                .createdOn(testResult.getCreatedOn() != null ? Instant.ofEpochSecond(testResult.getCreatedOn()) : null)
                .defects(Arrays.stream(StringUtils.defaultIfEmpty(testResult.getDefects(), "").split(",")).map(String::trim).filter(StringUtils::isNotEmpty).collect(Collectors.toList()))
                .comment(testResult.getComment())
                .elapsed(testResult.getElapsed())
                .status(MoreObjects.firstNonNull(testResult.getStatus(), UNASSIGNED))
                .build();
    }
}
