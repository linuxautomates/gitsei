package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.Questionnaire;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder(toBuilder = true, builderMethodName = "questionnaireDetailsBuilder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionnaireDetails extends Questionnaire {
    private String reason;
    private String questionnaireTemplateName;
    private String integrationApplication;
    private String integrationUrl;
    private String artifact;

    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;
}