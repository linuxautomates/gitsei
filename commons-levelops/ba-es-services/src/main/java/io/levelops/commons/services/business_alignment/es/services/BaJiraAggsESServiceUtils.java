package io.levelops.commons.services.business_alignment.es.services;

import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaJiraAggsESServiceUtils {
    public static List<String> getEffectiveCategoryNames(String categoryName, List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters) {
        //Populate all Category Names
        List<String> allCategoryNamesFromProfile = new ArrayList<>();
        allCategoryNamesFromProfile.addAll(CollectionUtils.emptyIfNull(ticketCategorizationFilters).stream().map(f -> f.getName()).collect(Collectors.toList()));
        allCategoryNamesFromProfile.add("Other");

        //If no category name is specified, return all category names
        if (StringUtils.isBlank(categoryName)) {
            return allCategoryNamesFromProfile;
        }
        if (allCategoryNamesFromProfile.contains(categoryName)) {
            //If specified category name exists in profile return it
            return List.of(categoryName);
        }
        throw new RuntimeException("Category Name " + categoryName + " does not exist in the profile!");
    }

    /**
     * UI sends AggInterval only for Across ISSUE_RESOLVED_AT. So use AggInterval from Filter only for Across ISSUE_RESOLVED_AT.
     * For all other across, UI send null but BE uses a default of "day" ¯\_(ツ)_/¯, so for all other across we use Agg Interval MONTH.
     * @param across
     * @param filter
     * @return
     */
    public static ESAggInterval determineAggInterval(JiraAcross across, JiraIssuesFilter filter) {
        if(JiraAcross.ISSUE_RESOLVED_AT != across) {
            return ESAggInterval.MONTH;
        }
        return ESAggInterval.fromStringWithDefault(filter.getAggInterval(), ESAggInterval.MONTH);
    }
}
