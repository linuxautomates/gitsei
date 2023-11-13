package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = QuestionnaireAggFilter.QuestionnaireAggFilterBuilder.class)
public class QuestionnaireAggFilter {

    // NB: this class and QuestionnaireFilter should eventually converge (must sync up with FE)

    @JsonProperty("across")
    Distinct across;
    @JsonProperty("calculation")
    Calculation calculation;

    // -- filters
    @JsonProperty("questionnaire_template_id")
    List<String> questionnaireTemplateId;
    @JsonProperty("assignees")
    List<String> assignees;
    @JsonProperty("unassigned")
    Boolean unassigned;
    @JsonProperty("completed")
    Boolean completed; // 100% answered
    @JsonProperty("submitted")
    Boolean submitted; // regardless of completion %
    @JsonProperty("tags")
    List<String> tags;
    @JsonProperty("work_item_tags")
    List<String> workItemTags;
    @JsonProperty("states")
    List<String> states;
    @JsonProperty("work_item_product_ids")
    List<String> workItemProductIds;
    @JsonProperty("updated_at")
    DateFilter updatedAt;
    @JsonProperty("created_at")
    DateFilter createdAt;

    public enum Distinct {
        questionnaire_template_id,
        assignee,
        completed, // 100% answered
        submitted, // submitted, regardless of completion %
        tag,
        work_item_tag,
        state,
        work_item_product,

        //these are across time
        created,
        updated,
        trend;

        public static Set<Distinct> STACKABLE = Set.of(questionnaire_template_id, assignee, completed, submitted, tag);

        @Nullable
        @JsonCreator
        public static Distinct fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Distinct.class, value);
        }
    }

    public enum Calculation {
        response_time, // min, max, median
        count; // just a count

        @Nullable
        @JsonCreator
        public static Calculation fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Calculation.class, value);
        }
    }

}