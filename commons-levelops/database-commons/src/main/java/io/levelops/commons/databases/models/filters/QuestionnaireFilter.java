package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Questionnaire;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = QuestionnaireFilter.QuestionnaireFilterBuilder.class)
public class QuestionnaireFilter {

    // NB: this class and QuestionnaireAggFilter should eventually converge (must sync up with FE)

    @JsonProperty("product_id")
    String productId;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("work_item_ids")
    List<UUID> workItemIds;

    @JsonProperty("questionnaire_template_ids")
    List<UUID> questionnaireTemplateIds;

    @JsonProperty("ids")
    List<UUID> ids;

    @JsonProperty("tag_ids")
    List<String> tagIds;

    @JsonProperty("target_email")
    String targetEmail;

    @JsonProperty("main")
    Boolean main;

    @JsonProperty("state")
    Questionnaire.State state;

    @JsonProperty("is_fully_answered")
    Boolean isFullyAnswered; // this looks at num qns answered vs total qns

    @JsonProperty("updated_at")
    DateFilter updatedAt;

    @JsonProperty("created_at")
    DateFilter createdAt;

    @JsonProperty("assignee_user_ids")
    List<String> assigneeUserIds;

}
