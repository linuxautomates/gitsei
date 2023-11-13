package io.levelops.api.utils;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
public class SalesforceCaseFilterUtils {
    @SuppressWarnings("unchecked")
    public static SalesforceCaseFilter buildFilter(final String company, DefaultListRequest filter, Long ingestedAt) throws BadRequestException {
        List<String> integrationIds = getListOrDefault(filter, "integration_ids");
        if (CollectionUtils.isEmpty(integrationIds))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");

        Map<String, String> sfCreatedRange = filter.getFilterValue("salesforce_created_at", Map.class)
                .orElse(Map.of());
        Map<String, String> sfUpdatedRange = filter.getFilterValue("salesforce_updated_at", Map.class)
                .orElse(Map.of());

        final Long sfCreateStart = sfCreatedRange.get("$gt") != null ? Long.valueOf(sfCreatedRange.get("$gt")) : null;
        final Long sfCreateEnd = sfCreatedRange.get("$lt") != null ? Long.valueOf(sfCreatedRange.get("$lt")) : null;
        final Long sfUpdateStart = sfUpdatedRange.get("$gt") != null ? Long.valueOf(sfUpdatedRange.get("$gt")) : null;
        final Long sfUpdateEnd = sfUpdatedRange.get("$lt") != null ? Long.valueOf(sfUpdatedRange.get("$lt")) : null;


        SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilter.builder()
                .ingestedAt(ingestedAt)
                .extraCriteria(MoreObjects.firstNonNull(
                        getListOrDefault(filter, "salesforce_hygiene_types"),
                        List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(SalesforceCaseFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .caseIds(getListOrDefault(filter, "salesforce_case_ids"))
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .caseNumbers(getListOrDefault(filter, "salesforce_case_numbers"))
                .priorities(getListOrDefault(filter, "salesforce_priorities"))
                .statuses(getListOrDefault(filter, "salesforce_statuses"))
                .contacts(getListOrDefault(filter, "salesforce_contacts"))
                .types(getListOrDefault(filter, "salesforce_types"))
                .age(filter.<String, Object>getFilterValueAsMap("salesforce_age").orElse(Map.of()))
                .integrationIds(integrationIds)
                .accounts(getListOrDefault(filter, "salesforce_accounts"))
                .across(MoreObjects.firstNonNull(SalesforceCaseFilter.DISTINCT.fromString(filter.getAcross()),
                        SalesforceCaseFilter.DISTINCT.trend))
                .SFCreatedRange(ImmutablePair.of(sfCreateStart, sfCreateEnd))
                .SFUpdatedRange(ImmutablePair.of(sfUpdateStart, sfUpdateEnd))
                .build();
        log.info("salesforceCaseFilter = {}", salesforceCaseFilter);
        return salesforceCaseFilter;
    }
}
