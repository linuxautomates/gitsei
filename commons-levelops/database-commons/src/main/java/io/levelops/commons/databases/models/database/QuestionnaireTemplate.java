package io.levelops.commons.databases.models.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Represents a questionnaire template that will be used to create questionnaire instances {@link Questionnaire}
 */
public class QuestionnaireTemplate {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("low_risk_boundary")
    @Default
    private Integer lowRiskBoundary = 35;
    @JsonProperty("mid_risk_boundary")
    @Default
    private Integer midRiskBoundary = 70;
    @JsonProperty("updated_at")
    private Long updatedAt;
    @JsonProperty("created_at")
    private Long createdAt;
    @JsonProperty("sections")
    @Default
    private List<UUID> sections = new ArrayList<>();

    @JsonProperty("risk_enabled")
    private Boolean riskEnabled;

    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;

    @Singular
    @JsonProperty("kb_ids")
    private List<UUID> kbIds;
}