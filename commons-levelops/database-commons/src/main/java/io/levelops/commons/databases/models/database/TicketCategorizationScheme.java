package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.MapUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketCategorizationScheme.TicketCategorizationSchemeBuilder.class)
public class TicketCategorizationScheme {

    public static final String DEFAULT_TICKET_CATEGORY = "Other";

    @JsonProperty("id")
    String id;

    @JsonProperty("default_scheme")
    Boolean defaultScheme;

    @JsonProperty("name")
    String name;

    @JsonProperty("config")
    TicketCategorizationConfig config;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant updatedAt;

    @JsonIgnore
    public TicketCategorizationConfig getConfig() {
        return config != null ? config : TicketCategorizationConfig.builder().build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketCategorizationConfig.TicketCategorizationConfigBuilder.class)
    public static class TicketCategorizationConfig {

        @JsonProperty("description")
        String description;

        @JsonProperty("integration_type")
        String integrationType;

        @JsonProperty("categories")
        Map<String, TicketCategorization> categories;

        @JsonProperty("uncategorized")
        Uncategorized uncategorized;

        @JsonProperty("active_work")
        ActiveWork activeWork;

        @JsonIgnore
        public Map<String, TicketCategorization> getCategories() {
            return MapUtils.emptyIfNull(categories);
        }

        @JsonIgnore
        public ActiveWork getActiveWork() {
            return activeWork != null ? activeWork : ActiveWork.builder().build();
        }

        @JsonIgnore
        public Uncategorized getUncategorized() {
            return uncategorized != null ? uncategorized : Uncategorized.builder().build();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketCategorization.TicketCategorizationBuilder.class)
    public static class TicketCategorization {

        @JsonProperty("id")
        String id;

        @JsonProperty("name")
        String name;

        @JsonProperty("description")
        String description;

        @JsonProperty("index")
        Integer index;

        @JsonProperty("color")
        String color;

        @JsonProperty("goals")
        Goals goals;

        @JsonProperty("filter")
        Map<String, Object> filter;

        @JsonProperty("metadata")
        Map<String, Object> metadata;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Uncategorized.UncategorizedBuilder.class)
    public static class Uncategorized {
        @JsonProperty("color")
        String color;
        @JsonProperty("goals")
        Goals goals;

        @JsonIgnore
        public Goals getGoals() {
            return goals != null ? goals : Goals.builder().build();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Goals.GoalsBuilder.class)
    public static class Goals {
        @JsonProperty("enabled")
        Boolean enabled;
        @JsonProperty("ideal_range")
        Goal idealRange;
        @JsonProperty("acceptable_range")
        Goal acceptableRange;

        @JsonIgnore
        public Goal getIdealRange() {
            return idealRange != null ? idealRange : Goal.builder().build();
        }

        @JsonIgnore
        public Goal getAcceptableRange() {
            return acceptableRange != null ? acceptableRange : Goal.builder().build();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Goal.GoalBuilder.class)
    @AllArgsConstructor
    public static class Goal {
        @JsonProperty("min")
        Integer min;
        @JsonProperty("max")
        Integer max;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ActiveWork.ActiveWorkBuilder.class)
    public static class ActiveWork {
        @JsonProperty("issues")
        IssuesActiveWork issues;

        @JsonIgnore
        public IssuesActiveWork getIssues() {
            return issues != null ? issues : IssuesActiveWork.builder().build();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IssuesActiveWork.IssuesActiveWorkBuilder.class)
    public static class IssuesActiveWork {
        @JsonProperty("active_sprints")
        Boolean activeSprints;
        @JsonProperty("active_releases")
        Boolean activeReleases;
        @JsonProperty("in_progress")
        Boolean inProgress;
        @JsonProperty("assigned")
        Boolean assigned;
    }

    public Optional<Goals> retrieveGoals(String category, String defaultTicketCategory) {
        if (StringUtils.isBlank(category)) {
            return Optional.empty();
        }
        Optional<Goals> goals;
        if (StringUtils.isBlank(category) || category.equalsIgnoreCase(defaultTicketCategory)) {
            goals = Optional.ofNullable(this.getConfig().getUncategorized().getGoals());
        } else {
            goals = this.getConfig().getCategories().values().stream()
                    .filter(cat -> category.equalsIgnoreCase(cat.getName()))
                    .findFirst()
                    .map(TicketCategorizationScheme.TicketCategorization::getGoals);
        }
        return goals.filter(g -> BooleanUtils.isTrue(g.getEnabled()));
    }

    public Optional<Goals> retrieveGoals(String category) {
        return retrieveGoals(category, DEFAULT_TICKET_CATEGORY);
    }

    public List<String> retrieveCategories(String defaultTicketCategory) {
        return ListUtils.addIfNotPresent(MapUtils.emptyIfNull(this.getConfig().getCategories()).values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(cat -> MoreObjects.firstNonNull(cat.getIndex(), Integer.MAX_VALUE)))
                .peek(cat -> {
                    if (cat.getIndex() == null) {
                        log.warn("Ticket categorization scheme with un-indexed category: schemeId={},  category={}", this.getId(), cat.getName());
                    }
                })
                .map(TicketCategorization::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()), defaultTicketCategory);
    }

    /**
     * Returns a list of category names, in the proper precedence order, including the default category.
     */
    public List<String> retrieveCategories() {
        return retrieveCategories(DEFAULT_TICKET_CATEGORY);
    }

}
