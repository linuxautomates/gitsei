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

@lombok.Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemFilter.WorkItemFilterBuilder.class)
public class WorkItemFilter {
    @JsonProperty("across")
    Distinct across;
    @JsonProperty("calculation")
    Calculation calculation;

    // region filters
    @JsonProperty("states")
    List<String> states;
    @JsonProperty("assignees")
    List<String> assignees;
    @JsonProperty("reporters")
    List<String> reporters;
    @JsonProperty("tags")
    List<String> tags;
    @JsonProperty("products")
    List<String> products;
    @JsonProperty("updated_at")
    UpdatedAtFilter updatedAt;
    // endregion

    public enum Distinct {
        state,
        assignee,
        reporter,
        tag,
        product,

        // across time
        created,
        updated,
        trend;

        public static Set<Distinct> STACKABLE = Set.of(
                state, assignee, reporter, tag, product);

        @Nullable
        @JsonCreator
        public static WorkItemFilter.Distinct fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(WorkItemFilter.Distinct.class, value);
        }
    }

    public enum Calculation {
        count;

        @Nullable
        @JsonCreator
        public static WorkItemFilter.Calculation fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(WorkItemFilter.Calculation.class, value);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = UpdatedAtFilter.UpdatedAtFilterBuilder.class)
    public static class UpdatedAtFilter {
        @JsonProperty("$lt")
        Long lt;
        @JsonProperty("$gt")
        Long gt;
    }
}
