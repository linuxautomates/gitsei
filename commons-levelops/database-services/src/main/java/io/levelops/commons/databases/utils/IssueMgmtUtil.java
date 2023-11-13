package io.levelops.commons.databases.utils;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.data.util.Pair;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Log4j2
@SuppressWarnings("unused")
public class IssueMgmtUtil {

    public static WorkItemsFilter resolveAcross(DefaultListRequest filter, WorkItemsFilter workItemsFilter) {
        if (workItemsFilter.getAcross() != null || StringUtils.isEmpty(filter.getAcross())) {
            return workItemsFilter;
        }
        boolean isCustomField = IssueMgmtCustomFieldUtils.isCustomField(filter.getAcross());
        return workItemsFilter.toBuilder()
                .across(isCustomField
                        ? WorkItemsFilter.DISTINCT.custom_field
                        : WorkItemsFilter.DISTINCT.attribute)
                .customAcross(isCustomField ? filter.getAcross() : null)
                .attributeAcross(!isCustomField ? filter.getAcross() : null)
                .build();
    }

    public static ImmutablePair<WorkItemsFilter, WorkItemsFilter.DISTINCT> resolveStack(DefaultListRequest filter, WorkItemsFilter workItemsFilter) {
        WorkItemsFilter.DISTINCT stack = null;
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            stack = WorkItemsFilter.DISTINCT.fromString(filter.getStacks().get(0));
            if (stack == null && IssueMgmtCustomFieldUtils.isCustomField(filter.getStacks().get(0))) {
                stack = WorkItemsFilter.DISTINCT.custom_field;
                workItemsFilter = workItemsFilter.toBuilder().customStack(filter.getStacks().get(0)).build();
            } else if (Objects.isNull(stack)) {
                stack = WorkItemsFilter.DISTINCT.attribute;
                workItemsFilter = workItemsFilter.toBuilder().attributeStack(filter.getStacks().get(0)).build();
            }
            return ImmutablePair.of(workItemsFilter, stack);
        } else {
            return ImmutablePair.of(workItemsFilter, stack);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, String>> scmFilesPartialMatchMap(DefaultListRequest filter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        return partialMatchMap.entrySet().stream()
                .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                .collect(Collectors.toMap(
                        stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                        Map.Entry::getValue));
    }

    public static void validatePartialMatchFilter(String company,
                                                  Map<String, Map<String, String>> partialMatchMap) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> !ScmAggService.FILES_PARTIAL_MATCH_COLUMNS.contains(key))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for scm file partial match based filter");
        }
    }

    @Nullable
    public static List<WorkItemsFilter.TicketCategorizationFilter> generateTicketCategorizationFilters(
            String company, @Nullable String ticketCategorizationSchemeId, TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService) throws BadRequestException {
        if (StringUtils.isEmpty(ticketCategorizationSchemeId)) {
            return null;
        }
        TicketCategorizationScheme ticketCategorizationScheme = ticketCategorizationSchemeDatabaseService.get(company, UUID.fromString(ticketCategorizationSchemeId).toString())
                .orElseThrow(() -> new BadRequestException("No ticket categorization scheme with this id=" + ticketCategorizationSchemeId));
        if (ticketCategorizationScheme.getConfig() == null || MapUtils.isEmpty(ticketCategorizationScheme.getConfig().getCategories())) {
            return null;
        }
        return generateTicketCategorizationFilters(company, ticketCategorizationScheme.getConfig().getCategories().values().stream().collect(Collectors.toList()));
    }

    @Nullable
    public static List<WorkItemsFilter.TicketCategorizationFilter> generateTicketCategorizationFilters(
            String company, @Nullable List<TicketCategorizationScheme.TicketCategorization> categories) throws BadRequestException {
        return categories.stream()
                .filter(category -> StringUtils.isNotEmpty(category.getName()))
                .map(category -> {
                    try {
                        Map<String, Object> categoryFilter = new HashMap<>(MapUtils.emptyIfNull(category.getFilter()));
                        categoryFilter.remove("ticket_categorization_scheme");
                        categoryFilter.remove("ticket_categories");
                        DefaultListRequest categoryFilterRequest = DefaultListRequest.builder()
                                .filter(categoryFilter)
                                .build();
                        return WorkItemsFilter.TicketCategorizationFilter.builder()
                                .id(category.getId())
                                .index(category.getIndex())
                                .name(category.getName())
                                .filter(WorkItemsFilter.fromDefaultListRequest(categoryFilterRequest, null, null))
                                .build();
                    } catch (Exception e) {
                        log.warn("Failed to parse ticket category filter from category=" + category.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static LatestIngestedAt getIngestedAt(String company, IntegrationType type, WorkItemsFilter filter, IntegrationService integrationService, LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache) throws SQLException {
        //if filter has ingested_at use it (do not make call to get integration)
        if (filter.getIngestedAt() != null) {
            return LatestIngestedAt.builder()
                    .latestIngestedAt(DateUtils.truncate(new Date(TimeUnit.SECONDS.toMillis(filter.getIngestedAt())), Calendar.DATE))
                    .build();
        }
        ImmutablePair<Long, Long> snapshotRange = MoreObjects.firstNonNull(filter.getSnapshotRange(), ImmutablePair.nullPair());
        if (WorkItemsFilter.DISTINCT.trend.equals(filter.getAcross())
                || snapshotRange.getLeft() != null
                || snapshotRange.getRight() != null) {
            return LatestIngestedAt.builder().build();
        }
        //Default IngestedAt is start of today's day
        long defaultIngestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        // filter out integrations that don't match the application type we want
        List<Integer> integrationIdsFilter = filter.getIntegrationIds().stream()
                .map(NumberUtils::toInt)
                .collect(Collectors.toList());
        List<String> integrationIds;
        try {
            integrationIds = PaginationUtils.streamThrowingRuntime(0, 1, page ->
                            integrationService.listByFilter(company, null, List.of(type.toString()),
                                    null, integrationIdsFilter, List.of(), page, 25).getRecords())
                    .map(Integration::getId)
                    .collect(Collectors.toList());
        } catch (RuntimeStreamException e) {
            throw new SQLException("Failed to filter integration ids", e);
        }
        // for each integration, find out the latest ingested at
        Map<String, Long> latestIngestedAtByIntegrationId = integrationIds.stream()
                .collect(Collectors.toMap(integrationId -> integrationId,
                        integrationId -> getIngestedAtFromCache(company, integrationId, ingestedAtCache).orElse(defaultIngestedAt),
                        (a, b) -> b));

        // If no integration is found, return the default ingested at.
        // Otherwise, for backward compatibility, single out the first integration's latest ingested at.
        Long latestIngestedAt = IterableUtils.getFirst(integrationIds)
                .map(latestIngestedAtByIntegrationId::get)
                .orElse(defaultIngestedAt);
        return LatestIngestedAt.builder()
                .latestIngestedAt(latestIngestedAt)
                .latestIngestedAtByIntegrationId(latestIngestedAtByIntegrationId)
                .build();
    }

    public static Optional<Long> getIngestedAtFromCache(String company, String integrationId, LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache) {
        try {
            return ingestedAtCache.get(Pair.of(company, integrationId));
        } catch (ExecutionException e) {
            log.warn("Failed to load ingestedAt for company=" + company + ", integrationId=" + integrationId, e);
            return Optional.empty();
        }
    }

    private static LoadingCache<Pair<String, String>, Optional<Long>> initIngestedAtCache(final IntegrationTrackingService integrationTrackingService) {
        return CacheBuilder.from("maximumSize=1000,expireAfterWrite=15m").build(CacheLoader.from(pair -> {
            String company = pair.getFirst();
            String integrationId = pair.getSecond();
            return integrationTrackingService.get(company, integrationId)
                    .map(IntegrationTracker::getLatestIngestedAt);
        }));
    }
}

