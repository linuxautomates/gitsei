package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSonarQubeCoverage.DbSonarQubeCoverageBuilder.class)
public class DbSonarQubeCoverage {

    private static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("id")
    String id;

    @JsonProperty("parent_id")
    String parentId;

    @JsonProperty("coverage")
    String coverage;

    @JsonProperty("line_coverage")
    String line_coverage;

    @JsonProperty("lines")
    String lines;

    @JsonProperty("uncovered_lines")
    String uncovered_lines;

    @JsonProperty("covered_lines")
    String covered_lines;

    @JsonProperty("conditions_to_cover")
    String conditions_to_cover;

    @JsonProperty("uncovered_conditions")
    String uncovered_conditions;

    @JsonProperty("covered_conditions")
    String covered_conditions;

    @JsonProperty("new_coverage")
    String new_coverage;

    @JsonProperty("new_line_coverage")
    String new_line_coverage;

    @JsonProperty("new_lines")
    String new_lines;

    @JsonProperty("new_uncovered_lines")
    String new_uncovered_lines;

    @JsonProperty("new_covered_lines")
    String new_covered_lines;

    @JsonProperty("new_conditions_to_cover")
    String new_conditions_to_cover;

    @JsonProperty("new_uncovered_conditions")
    String new_uncovered_conditions;

    @JsonProperty("new_covered_conditions")
    String new_covered_conditions;

    @JsonProperty("parent")
    String parent;

    @JsonProperty("repo")
    String repo;
}
