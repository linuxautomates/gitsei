package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TestRailsTestSuite.TestRailsTestSuiteBuilder.class)
public class TestRailsTestSuite {

    @JsonProperty("id")
    Integer id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("project_id")
    Integer projectId;
    @JsonProperty("is_master")
    Boolean isMaster;
    @JsonProperty("is_baseline")
    Boolean isBaseline;
    @JsonProperty("is_completed")
    Boolean isCompleted;
    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("completed_on")
    Date completedOn;
    @JsonProperty("url")
    String url;
    @JsonProperty("test_cases")
    List<TestRailsTestCase> testCases;
}