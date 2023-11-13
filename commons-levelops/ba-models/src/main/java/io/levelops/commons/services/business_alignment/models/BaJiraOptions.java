package io.levelops.commons.services.business_alignment.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BaJiraOptions.BaJiraOptionsBuilder.class)

public class BaJiraOptions {
    // to customize "completed work" statuses (defaults to status_category=DONE)
    List<String> completedWorkStatuses;
    List<String> completedWorkStatusCategories;

    // to customize "in progress" statuses (used by ticket time spent metric)
    List<String> inProgressStatuses;
    List<String> inProgressStatusCategories;

    // attribution mode
    AttributionMode attributionMode;
    List<String> historicalAssigneesStatuses;

    public AttributionMode getAttributionMode() {
        return ObjectUtils.firstNonNull(attributionMode, AttributionMode.CURRENT_ASSIGNEE);
    }

    public enum AttributionMode {
        CURRENT_ASSIGNEE, // default
        CURRENT_AND_PREVIOUS_ASSIGNEES;

        @JsonCreator
        @Nullable
        public static AttributionMode fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(AttributionMode.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public String generateCacheHashRawString() {
        StringBuilder dataToHash = new StringBuilder();
        CacheHashUtils.hashData(dataToHash, "completedWorkStatuses", completedWorkStatuses);
        CacheHashUtils.hashData(dataToHash, "completedWorkStatusCategories", completedWorkStatusCategories);
        CacheHashUtils.hashData(dataToHash, "inProgressStatuses", inProgressStatuses);
        CacheHashUtils.hashData(dataToHash, "inProgressStatusCategories", inProgressStatusCategories);
        CacheHashUtils.hashData(dataToHash, "attributionMode", attributionMode);
        CacheHashUtils.hashData(dataToHash, "historicalAssigneesStatuses", historicalAssigneesStatuses);
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheHashRawString());
    }

    public static BaJiraOptions fromDefaultListRequest(DefaultListRequest request) {
        return BaJiraOptions.builder()
                .completedWorkStatusCategories(request.<String>getFilterValueAsList("ba_completed_work_status_categories").orElse(null))
                .completedWorkStatuses(request.<String>getFilterValueAsList("ba_completed_work_statuses").orElse(null))
                .inProgressStatusCategories(request.<String>getFilterValueAsList("ba_in_progress_status_categories").orElse(null))
                .inProgressStatuses(request.<String>getFilterValueAsList("ba_in_progress_statuses").orElse(null))
                .attributionMode(request.<String>getFilterValue("ba_attribution_mode", String.class).map(AttributionMode::fromString).orElse(AttributionMode.CURRENT_ASSIGNEE))
                .historicalAssigneesStatuses(request.<String>getFilterValueAsList("ba_historical_assignees_statuses").orElse(null))
                .build();
    }
}
