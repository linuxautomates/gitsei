package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Question {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "severity")
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @JsonProperty(value = "section_id")
    @JsonAlias(value = "question_id")
    private String sectionId; // parent id

    @JsonProperty(value = "type")
    private String type;

    @Singular
    @JsonProperty(value = "options")
    private List<Option> options;

    @JsonProperty("verifiable")
    @Builder.Default
    private Boolean verifiable = Boolean.FALSE;

    @JsonProperty(value = "verification_mode")
    private ActionMode verificationMode;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("required")
    private Boolean required;

    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Option {
        @JsonProperty(value = "value")
        private String response;

        @JsonProperty(value = "score")
        private Integer score;

        @JsonProperty(value = "editable")
        private Boolean editable;
    }
}