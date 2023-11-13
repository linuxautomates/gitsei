package io.levelops.integrations.awsdevtools.models;

import com.amazonaws.services.codebuild.model.Report;
import com.amazonaws.services.codebuild.model.ReportGroup;
import com.amazonaws.services.codebuild.model.TestCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CBReport.CBReportBuilder.class)
public class CBReport {

    @NonNull
    @JsonProperty("report")
    Report report;

    @JsonProperty("reportGroup")
    ReportGroup reportGroup;

    @JsonProperty("testCases")
    List<TestCase> testCases;
}
