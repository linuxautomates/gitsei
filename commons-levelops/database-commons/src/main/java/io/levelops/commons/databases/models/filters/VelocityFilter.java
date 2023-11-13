package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityFilter.VelocityFilterBuilder.class)
public class VelocityFilter {
    private static final boolean DOES_SUPPORT_STACKS = true;
    private static final boolean DOES_NOT_SUPPORT_STACKS = false;
    private static final boolean DOES_SUPPORT_STACK_VALUES = true;
    private static final boolean DOES_NOT_SUPPORT_STACK_VALUES = false;
    @JsonProperty("across")
    DISTINCT across;
    @JsonProperty("calculation")
    CALCULATION calculation;
    @JsonProperty("stacks")
    List<STACK> stacks;
    
    /*
    For stacks when we do drilldown, fe will send selected values
    For any stack, we reuse this field
     */
    @JsonProperty("value_stacks")
    List<String> valueStacks; //Will be reused for WI too

    @JsonProperty("value_trend_keys")
    List<Long> valueTrendKeys;

    @JsonProperty("stage_name")
    String histogramStageName;

    @JsonProperty("histogram_buckets_count")
    Integer histogramBucketsCount;

    @JsonProperty("ratings")
    List<VelocityConfigDTO.Rating> ratings;

    /*
    By default we will use all tickets or PRs for calculation.
    If we want to use only the tickets or PRs applicable to that stage we use
     */
    @JsonProperty("limit_to_only_applicable_data")
    Boolean limitToOnlyApplicableData;

    @JsonProperty("sort")
    Map<String, SortingOrder> sort;

    @JsonProperty("page")
    Integer page;
    @JsonProperty("page_size")
    Integer pageSize;

    @Getter
    public enum DISTINCT {
        velocity(DOES_SUPPORT_STACKS, DOES_NOT_SUPPORT_STACK_VALUES),
        trend(DOES_NOT_SUPPORT_STACKS, DOES_NOT_SUPPORT_STACK_VALUES),
        values(DOES_NOT_SUPPORT_STACKS, DOES_SUPPORT_STACK_VALUES),
        histogram(DOES_NOT_SUPPORT_STACKS, DOES_SUPPORT_STACK_VALUES),
        rating(DOES_SUPPORT_STACKS,DOES_NOT_SUPPORT_STACK_VALUES);

        @JsonProperty("supports_stacks")
        private final boolean supportsStacks;

        @JsonProperty("supports_stack_values")
        private final boolean supportsStackValues;

        DISTINCT(boolean supportsStacks, boolean supportsStackValues) {
            this.supportsStacks = supportsStacks;
            this.supportsStackValues = supportsStackValues;
        }

        @JsonCreator
        @Nullable
        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Getter
    public enum CALCULATION {
        ticket_velocity,
        pr_velocity;

        @JsonCreator
        @Nullable
        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Getter
    public enum STACK {
        issue_type(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity)), //STACK issue_type is supported only for DISTINCT velocity & not supported for DISTINCT trend, Also this stack is supported only for calculation ticket_velocity & not for pr_velocity
        issue_priority(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity)),
        issue_component(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity)),
        issue_project(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity)),
        issue_label(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity)),
        issue_epic(Set.of(DISTINCT.velocity), Set.of(DISTINCT.values, DISTINCT.histogram), Set.of(CALCULATION.ticket_velocity));

        @JsonProperty("supported_across")
        private final Set<DISTINCT> supportedAcross;
        @JsonProperty("supported_across_for_values")
        private final Set<DISTINCT> supportedAcrossForValues;
        @JsonProperty("supported_calculations")
        private final Set<CALCULATION> supportedCalculations;

        STACK(Set<DISTINCT> supportedAcross, Set<DISTINCT> supportedAcrossForValues, Set<CALCULATION> supportedCalculations) {
            this.supportedAcross = supportedAcross;
            this.supportedAcrossForValues = supportedAcrossForValues;
            this.supportedCalculations = supportedCalculations;
        }


        @JsonCreator
        @Nullable
        public static STACK fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(STACK.class, st);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    public static VelocityFilter fromListRequest(DefaultListRequest filter) {
        List<STACK> stacks = CollectionUtils.emptyIfNull(filter.getStacks()).stream().map(STACK::fromString).filter(Objects::nonNull).collect(Collectors.toList());
        //Map<String, String> rnge = request.getFilterValue(key, Map.class).orElse(Map.of());
        List<String> valueStacks = getListOrDefault(filter, "value_stacks");
        // Temp Code for Backward Compatibility - Start
        if(CollectionUtils.isEmpty(valueStacks)) {
            List<String> valueJiraIssueTypes = getListOrDefault(filter, "value_jira_issue_types");
            if(CollectionUtils.isNotEmpty(valueJiraIssueTypes)) {
                //If value_stacks is not provided & if value_jira_issue_types is provided, then we have older contract
                //We will set value_stacks = value_jira_issue_types and set stacks to issue_type
                valueStacks = valueJiraIssueTypes;
                stacks = List.of(STACK.issue_type);
            }
        }
        // Temp Code for Backward Compatibility - End
        return VelocityFilter.builder()
                .across(DISTINCT.fromString(filter.getAcross()))
                .calculation(CALCULATION.fromString(filter.getFilterValue("calculation", String.class).orElse(null)))
                .stacks(stacks)
                .valueStacks(valueStacks)
                .valueTrendKeys(getListOrDefault(filter, "value_trend_keys").stream().map(Long::parseLong).collect(Collectors.toList()))
                .limitToOnlyApplicableData(Boolean.TRUE.equals(filter.getFilterValue("limit_to_only_applicable_data", Boolean.class).orElse(null)))
                .histogramStageName(filter.getFilterValue("histogram_stage_name", String.class).orElse(null))
                .histogramBucketsCount(filter.getFilterValue("histogram_buckets_count", Integer.class).orElse(null))
                .ratings(filter.getFilterValueAsList("ratings").orElse(List.of()).stream()
                        .map(Object::toString)
                        .map(VelocityConfigDTO.Rating::fromString).collect(Collectors.toList()))
                .sort(SortingConverter.fromFilter(filter.getSort()))
                .page(filter.getPage())
                .pageSize(filter.getPageSize())
                .build();
    }
}
