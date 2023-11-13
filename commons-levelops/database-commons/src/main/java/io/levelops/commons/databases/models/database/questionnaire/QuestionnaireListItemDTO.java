package io.levelops.commons.databases.models.database.questionnaire;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionnaireListItemDTO {
    private String id;
    private String workItemId;
    private String ticketId;
    private String reason;
    @JsonProperty("questionnaire_template_id")
    private String questionnaireTemplateId;
    @JsonProperty("questionnaire_template_name")
    private String questionnaireTemplateName;
    private Integer currentScore;
    private Severity priority;
    @JsonProperty("product_id")
    private String productId;
    private Integer totalQuestions;
    private String targetEmail;
    private Integer answeredQuestions;
    private Long updatedAt;
    private Long createdAt;
    private Boolean completed;

    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;

    @JsonProperty("state")
    private Questionnaire.State state;
    @JsonProperty("main")
    private Boolean main;

    @JsonProperty("sections")
    @Builder.Default
    private List<Section> sections = Collections.emptyList();
    @JsonProperty("section_responses")
    @Builder.Default
    private List<SectionResponse> answers = Collections.emptyList();
}