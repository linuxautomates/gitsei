package io.levelops.api.controllers;

import com.google.common.annotations.VisibleForTesting;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.filters.ActivityLogsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.database.ActivityLog.Action.*;
import static io.levelops.commons.databases.models.database.ActivityLog.TargetItemType.*;

@RestController
@RequestMapping("/v1/activitylogs")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class ActivityLogsController {

    /**
     * Set of ItemType/Action pairs where details can be returned by the API.
     * DO NOT ADD ANYTHING WITHOUT REVIEWING OR SANITIZING THE DETAILS!
     * THERE IS SENSITIVE DATA THAT COULD LEAK FROM THE DB!
     * <a href="https://harness.atlassian.net/browse/SEI-2608">SEI-2608</a>
     */
    private final static Set<Pair<ActivityLog.TargetItemType, ActivityLog.Action>> ITEM_ACTION_PAIRS_WITH_DETAILS_ENABLED = Set.of(
            Pair.of(DASHBOARD, EDITED)
    );

    private final ActivityLogService logService;

    @Autowired
    public ActivityLogsController(final ActivityLogService logService) {
        this.logService = logService;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ActivityLog>>> logList(@SessionAttribute("company") final String company,
                                                                                  @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> targetItems = getListOrDefault(filter.getFilter(), "target_items");
            List<String> emails = getListOrDefault(filter.getFilter(), "emails");
            List<String> actions = getListOrDefault(filter.getFilter(), "actions");
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));

            DbListResponse<ActivityLog> response = logService.list(company, targetItems, emails, actions, partialMatchMap, filter.getPage(), filter.getPageSize());

           // sanitize
            response = DbListResponse.<ActivityLog>builder()
                    .totalCount(response.getTotalCount())
                    .totals(response.getTotals())
                    .calculatedAt(response.getCalculatedAt())
                    .records(ListUtils.emptyIfNull(response.getRecords()).stream()
                            .map(ActivityLogsController::sanitizeActivityLog)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build();

            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), response));
        });
    }

    @Nullable
    @VisibleForTesting
    protected static ActivityLog sanitizeActivityLog(@Nullable ActivityLog log) {
        if (log == null) {
            return null;
        }
        ActivityLog.TargetItemType itemType = log.getTargetItemType();
        ActivityLog.Action action = log.getAction();
        Map<String, Object> details = Map.of();
        if (itemType != null && action != null && ITEM_ACTION_PAIRS_WITH_DETAILS_ENABLED.contains(Pair.of(itemType, action))) {
            details = log.getDetails();
        }
        return log.toBuilder()
                .details(details)
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, path = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
            for (String field : filter.getFields()) {
                response.add(Map.of(field, logService.groupByAndCalculate(
                        company,
                        ActivityLogsFilter.builder()
                                .across(ActivityLogsFilter.DISTINCT.fromString(field))
                                .targetItems(getListOrDefault(filter.getFilter(), "target_items"))
                                .emails(getListOrDefault(filter.getFilter(), "emails"))
                                .actions(getListOrDefault(filter.getFilter(), "actions"))
                                .partialMatch(partialMatchMap)
                                .build()
                ).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }
}