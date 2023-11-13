package io.levelops.commons.services.business_alignment.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

@AllArgsConstructor
@NoArgsConstructor
public enum JiraAcross {
    ASSIGNEE,
    TICKET_CATEGORY,
    TREND("ingested_at"),
    ISSUE_CREATED_AT,
    ISSUE_UPDATED_AT,
    ISSUE_RESOLVED_AT;

    String acrossColumnName;

    public String getAcrossColumnName() {
        return StringUtils.defaultIfEmpty(acrossColumnName, toString());
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    @Nullable
    public static JiraAcross fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(JiraAcross.class, value);
    }

    /*
    Later
    public static JiraAcross fromJiraFilter(JiraIssuesFilter filter) {
        JiraIssuesFilter.DISTINCT requestAcross = MoreObjects.firstNonNull(filter.getAcross(), JiraIssuesFilter.DISTINCT.ticket_category);
        switch (requestAcross) {
            case trend:
                return TREND;
            case issue_created:
                return ISSUE_CREATED_AT;
            case issue_updated:
                return ISSUE_UPDATED_AT;
            case issue_resolved:
                return ISSUE_RESOLVED_AT;
            case assignee:
                return ASSIGNEE;
            default:
            case ticket_category:
                return TICKET_CATEGORY;
        }
    }
     */
}