package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityProfile.DevProductivityProfileBuilder.class)
public class DevProductivityProfile {
    private static final Boolean SLOW_TO_GOOD_IS_ASCENDING = true;
    private static final Boolean SLOW_TO_GOOD_IS_DESCENDING = false;
    private static final Long DEFAULT_VALUE_IS_NULL = null; //If result data is empty, feature result is null, used for time based features. e.g. Avg Issue Response Time
    private static final Long DEFAULT_VALUE_IS_ZERO = 0L; //If result data is empty, feature result is zero, used for count based features. e.g. Number of Commits Per Month
    private static final Boolean TICKET_CATEGORY_NEEDED = true;
    private static final Boolean TICKET_CATEGORY_NOT_NEEDED = false;
    private static final Boolean DEPENDS_ON_TIME_INTERVAL = true;
    private static final Boolean DOES_NOT_DEPEND_ON_TIME_INTERVAL = false;
    private static final Boolean RELEASED = true;
    private static final Boolean NOT_RELEASED = false;

    @JsonProperty("id")
    private final UUID id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("description")
    private final String description;
    @JsonProperty("default_profile")
    private final Boolean defaultProfile;
    @JsonProperty("predefined_profile")
    private final Boolean predefinedProfile;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("matching_criteria")
    private List<MatchingCriteria> matchingCriteria;

    @JsonProperty("order")
    private final Integer order;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("settings")
    Map<String, Object> settings;

    @JsonProperty("effort_investment_profile_id")
    private final UUID effortInvestmentProfileId;

    @JsonProperty("associated_ou_ref_ids")
    private final List<String> associatedOURefIds;

    @JsonProperty("sections")
    private final List<Section> sections;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = MatchingCriteria.MatchingCriteriaBuilder.class)
    @EqualsAndHashCode
    public static final class MatchingCriteria {
        @JsonProperty("field")
        private final String field;

        @JsonProperty("values")
        private final List<String> values;

        @JsonProperty("condition")
        private final MatchingCondition condition;
    }

    @Getter
    public enum MatchingCondition {
        EQ("eq"),
        NEQ("new");

        private final String condition;

        MatchingCondition(String condition) {
            this.condition = condition;
        }

        public static MatchingCondition fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(MatchingCondition.class, st);
        }

        @Override
        public String toString() {
            return this.getCondition();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Section.SectionBuilder.class)
    public static final class Section {
        @JsonProperty("id")
        private final UUID id;

        @JsonProperty("name")
        private final String name;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("order")
        private final Integer order;

        @JsonProperty("enabled")
        private Boolean enabled;

        @Builder.Default
        @JsonProperty("weight")
        private final Integer weight = 10;

        @JsonProperty("features")
        private final List<Feature> features;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Feature.FeatureBuilder.class)
    public static final class Feature{
        @JsonProperty("id")
        private final UUID id;

        @JsonProperty("name")
        private final String name;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("order")
        private final Integer order;

        @JsonProperty("type")
        private final FeatureType featureType;

        @JsonProperty("max_value")
        private final Long maxValue;

        @JsonProperty("max_unit")
        private final TimeUnit maxUnit;

        @JsonProperty("lower_limit_percentage")
        private final Integer lowerLimitPercentage;

        @JsonProperty("upper_limit_percentage")
        private final Integer upperLimitPercentage;

        @JsonProperty("ratings")
        private final List<Rating> ratings;

        @JsonProperty("params")
        private final Map<String, List<String>> params;

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("ticket_categories")
        private final List<UUID> ticketCategories;

        private static final Integer MAX_REF_MULTIPLICATION_FACTOR = 10;
        private static final Integer MAX_SCORE = 90;

        public Boolean getSlowToGoodIsAscending () {
            return featureType.isSlowToGoodIsAscending();
        }
        public String getFeatureUnit() {
            return featureType.getFeatureUnit();
        }
        public String getFeatureMaxValueText(){
            return featureType.getMaxValueText();
        }
        public Rating calculateRating(Long valueCountOrInSeconds) {
            if (valueCountOrInSeconds == null) {
                return null;
            }

            Integer score = calculateScore(valueCountOrInSeconds);
            if (score < lowerLimitPercentage) {
                return Rating.NEEDS_IMPROVEMENT;
            } else if (score <= upperLimitPercentage) {
                return Rating.ACCEPTABLE;
            } else {
                return Rating.GOOD;
            }
        }

        private Integer calculateScoreForCounts(Long valueCount) {

            double maxRefValue = maxValue * MAX_REF_MULTIPLICATION_FACTOR;

            if(featureType.isSlowToGoodIsAscending()) {

                if(valueCount <= maxValue)
                    return Math.min(MAX_SCORE, (int) ((valueCount*MAX_SCORE)/maxValue));

                if(valueCount >= maxRefValue)
                    return 99;

                double  additionalCountPct =  valueCount * 100 / maxRefValue;
                int addOnPct = (int) (additionalCountPct * MAX_REF_MULTIPLICATION_FACTOR / 100);
                return MAX_SCORE + addOnPct;

            } else {

                if(valueCount == 0)
                   return MAX_SCORE;
                return 100 - (Math.min(100, (int) ((valueCount*100)/maxValue)));
            }
        }
        private Integer calculateScoreForTimings(Long valueInSeconds, TimeUnit maxUnit) {

            double maxRefValue = maxValue * MAX_REF_MULTIPLICATION_FACTOR;
            maxRefValue = maxUnit.toSeconds((long) maxRefValue);
            if(featureType.isSlowToGoodIsAscending()) {

                if(valueInSeconds <= maxUnit.toSeconds(maxValue))
                    return Math.min(100, (int) ((valueInSeconds*MAX_SCORE)/maxUnit.toSeconds(maxValue)));

                if(valueInSeconds >= maxRefValue)
                    return 99;

                double  additionalCountPct =  valueInSeconds * 100 / maxRefValue;
                int addOnPct = (int) (additionalCountPct * MAX_REF_MULTIPLICATION_FACTOR / 100);
                return MAX_SCORE + addOnPct;
            } else {
                if(valueInSeconds == 0)
                    return MAX_SCORE;
                return 100 - (Math.min(100, (int) ((valueInSeconds*100)/maxUnit.toSeconds(maxValue))));
            }
        }

        public Integer calculateScore(Long valueCountOrInSeconds) {
            if (valueCountOrInSeconds == null) {
                return null;
            }

            TimeUnit timeUnit = maxUnit;
            if(timeUnit == null && this.getFeatureUnit() != null && this.getFeatureUnit().equalsIgnoreCase("DAYS"))
                timeUnit = TimeUnit.DAYS;


            if (timeUnit == null) {
                return calculateScoreForCounts(valueCountOrInSeconds);
            } else {
                return calculateScoreForTimings(valueCountOrInSeconds, timeUnit);
            }
        }
    }

    @Getter
    public enum FeatureType{
        PRS_AVG_APPROVAL_TIME("Average response time for PR approvals", SLOW_TO_GOOD_IS_DESCENDING,TimeUnit.DAYS.name(),"Max # of Days", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        PRS_AVG_COMMENT_TIME("Average response time for PR comments", SLOW_TO_GOOD_IS_DESCENDING,TimeUnit.DAYS.name(),"Max # of Days", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        PERCENTAGE_OF_REWORK("Percentage of Rework", SLOW_TO_GOOD_IS_DESCENDING,"%","Max % of work", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        PERCENTAGE_OF_LEGACY_REWORK("Percentage of Legacy Rework", SLOW_TO_GOOD_IS_DESCENDING,"%","Max % of work", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        NUMBER_OF_PRS_PER_MONTH("Number of PRs", SLOW_TO_GOOD_IS_ASCENDING,"PRs","Top # of PRs", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_COMMITS_PER_MONTH("Number of Commits", SLOW_TO_GOOD_IS_ASCENDING,"Commits","Top # of Commits", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        LINES_OF_CODE_PER_MONTH("Lines of Code", SLOW_TO_GOOD_IS_ASCENDING,"Lines","Top # of lines", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_BUGS_FIXED_PER_MONTH("Number of bugs worked on", SLOW_TO_GOOD_IS_ASCENDING,"Bugs","Top # of Bugs", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_STORIES_RESOLVED_PER_MONTH("Number of stories worked on", SLOW_TO_GOOD_IS_ASCENDING,"Stories","Top # of Stories", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH("Number of Story Points worked on", SLOW_TO_GOOD_IS_ASCENDING,"Points","Top # of Points", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        BUGS_PER_HUNDRED_LINES_OF_CODE("Bugs per 100 Lines of code", SLOW_TO_GOOD_IS_DESCENDING,"Bugs","Max # of  Bugs", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, NOT_RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        TECHNICAL_BREADTH("Number of unique file extension", SLOW_TO_GOOD_IS_ASCENDING,"","Top # of Extensions", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        REPO_BREADTH("Number of unique repo", SLOW_TO_GOOD_IS_ASCENDING,"Repos","Top # of Repos", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        AVG_CODING_DAYS_PER_WEEK("Average Coding Days Per Week", SLOW_TO_GOOD_IS_ASCENDING,TimeUnit.DAYS.name(), "Top # of Days", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        AVG_PR_CYCLE_TIME("Average PR Cycle Time", SLOW_TO_GOOD_IS_DESCENDING,TimeUnit.DAYS.name(), "Max # of Days", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        AVG_ISSUE_RESOLUTION_TIME("Average Issue Resolution Time", SLOW_TO_GOOD_IS_DESCENDING,TimeUnit.DAYS.name(), "max # of Days", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH("High Impact bugs worked on",SLOW_TO_GOOD_IS_ASCENDING,"Bugs","Top # of Bugs", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH("High Impact stories worked on",SLOW_TO_GOOD_IS_ASCENDING,"Stories","Top # of Stories", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_PRS_APPROVED_PER_MONTH("Number of PRs approved",SLOW_TO_GOOD_IS_ASCENDING,"PRs","Top # of PRs", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH("Number of PRs commented on",SLOW_TO_GOOD_IS_ASCENDING,"PRs","Top # of PRs", DEFAULT_VALUE_IS_ZERO, TICKET_CATEGORY_NOT_NEEDED, RELEASED, DEPENDS_ON_TIME_INTERVAL),
        PRS_REVIEW_DEPTH ("PR Review Depth", SLOW_TO_GOOD_IS_DESCENDING,"","Max Depth", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, NOT_RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE("Sonar Bug Issues per 100 Lines of code", SLOW_TO_GOOD_IS_DESCENDING, "sonarBugs","Max # of sonarBugsBugs", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, NOT_RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE("Sonar Vulnerability Issues per 100 Lines of code", SLOW_TO_GOOD_IS_DESCENDING, "vulnerabilities","Max # of vulnerabilities", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, NOT_RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL),
        SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE("Sonar Bug Code Smells per 100 Lines of code", SLOW_TO_GOOD_IS_DESCENDING, "codeSmells","Max # of codeSmells", DEFAULT_VALUE_IS_NULL, TICKET_CATEGORY_NOT_NEEDED, NOT_RELEASED, DOES_NOT_DEPEND_ON_TIME_INTERVAL);

        private final String displayText;
        private final boolean slowToGoodIsAscending;
        private final String featureUnit;
        private final String maxValueText;
        private final Long defaultValue;
        private final boolean ticketCategoryNeeded;
        private final boolean released;
        private final boolean dependsOnTimeInterval;

        FeatureType(String displayText, boolean slowToGoodIsAscending, String featureUnit, String maxValueText, Long defaultValue, boolean ticketCategoryNeeded, boolean released, boolean dependsOnTimeInterval) {
            this.displayText = displayText;
            this.slowToGoodIsAscending = slowToGoodIsAscending;
            this.featureUnit = featureUnit;
            this.maxValueText = maxValueText;
            this.defaultValue = defaultValue;
            this.ticketCategoryNeeded = ticketCategoryNeeded;
            this.released = released;
            this.dependsOnTimeInterval = dependsOnTimeInterval;
        }

        public static FeatureType fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(FeatureType.class, st);
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        public String getDisplayTextForTimeInterval(ReportIntervalType interval) {
            return  displayText + interval.getLabelSuffix();
        }
    }

    @Getter
    public enum Rating {
        GOOD("Good"),
        ACCEPTABLE("Acceptable"),
        NEEDS_IMPROVEMENT("Needs Improvement");

        private final String displayText;

        Rating(String displayText) {
            this.displayText = displayText;
        }

        public static Rating fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(Rating.class, st);
        }

        @Override
        public String toString() {
            return this.getDisplayText();
        }
    }

    public static Map<Integer, Map<Integer, Feature>> buildFeaturesMap (final DevProductivityProfile profile) {
        if( profile == null ) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<Integer, Feature>> featureMap = new HashMap<>();
        for(Section s : CollectionUtils.emptyIfNull(profile.getSections())) {
            for(Feature f : CollectionUtils.emptyIfNull(s.getFeatures())) {
                featureMap.computeIfAbsent(s.getOrder(),k -> new HashMap<>()).put(f.getOrder(), f);
            }
        }
        return featureMap;
    }
    public static Set<FeatureType> buildFeatureTypesSet (final DevProductivityProfile profile) {
        if( profile == null ) {
            return Collections.emptySet();
        }
        Set<FeatureType> allFeatureTypes = new HashSet<>();
        for(Section s : CollectionUtils.emptyIfNull(profile.getSections())) {
            for(Feature f : CollectionUtils.emptyIfNull(s.getFeatures())) {
                allFeatureTypes.add(f.getFeatureType());
            }
        }
        return allFeatureTypes;
    }

    public static Integer getProfileWeightsTotal(final DevProductivityProfile devProductivityProfile) {
        Integer profileWeightsTotal = CollectionUtils.emptyIfNull(devProductivityProfile.getSections()).stream()
                .filter(section -> Boolean.TRUE.equals(section.getEnabled()))
                .mapToInt(Section::getWeight)
                .sum();
        return profileWeightsTotal;
    }
}
