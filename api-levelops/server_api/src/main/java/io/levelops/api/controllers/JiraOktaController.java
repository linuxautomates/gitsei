package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.OktaGroupsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraOktaService;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/jira_okta")
@SuppressWarnings("unused")
public class JiraOktaController {

    private final IntegrationService integrationService;
    private final JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
    private final JiraOktaService aggService;
    private final IntegrationTrackingService integrationTrackingService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public JiraOktaController(IntegrationService integrationService,
                              JiraFieldConditionsBuilder jiraFieldConditionsBuilder,
                              JiraOktaService aggService,
                              IntegrationTrackingService integrationTrackingService,
                              final OrgUnitHelper orgUnitHelper) {
        this.integrationService = integrationService;
        this.jiraFieldConditionsBuilder = jiraFieldConditionsBuilder;
        this.aggService = aggService;
        this.integrationTrackingService = integrationTrackingService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "groups/agg", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> groupsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.OKTA), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_okta' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            JiraIssuesFilter jiraFilter = getJiraIssuesFilterBuilderWithExcludedFilter(request)
                    .hygieneCriteriaSpecs(getHygieneCriteriaSpecs(request.getFilter()))
                    .ingestedAt(getIngestedAt(company, IntegrationType.JIRA, request))
                    .extraCriteria(MoreObjects.firstNonNull(
                            getListOrDefault(request.getFilter(), "jira_hygiene_types"),
                            List.of())
                            .stream()
                            .map(String::valueOf)
                            .map(JiraIssuesFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .keys(getListOrDefault(request.getFilter(), "jira_keys"))
                    .priorities(getListOrDefault(request.getFilter(), "jira_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "jira_statuses"))
                    .assignees(getListOrDefault(request.getFilter(), "jira_assignees"))
                    .reporters(getListOrDefault(request.getFilter(), "jira_reporters"))
                    .issueTypes(getListOrDefault(request.getFilter(), "jira_issue_types"))
                    .epics(getListOrDefault(request.getFilter(), "jira_epics"))
                    .integrationIds(integrationIds)
                    .projects(getListOrDefault(request.getFilter(), "jira_projects"))
                    .components(getListOrDefault(request.getFilter(), "jira_components"))
                    .labels(getListOrDefault(request.getFilter(), "jira_labels"))
                    .firstAssignees(getListOrDefault(request.getFilter(),"jira_first_assignees"))
                    .customFields((Map<String, Object>) request.getFilter().get("jira_custom_fields"))
                    .missingFields(MapUtils.emptyIfNull(
                            (Map<String, Boolean>) request.getFilter().get("jira_missing_fields")))
                    .summary((String) request.getFilter().getOrDefault("jira_summary", null))
                    .fieldSize(fieldSizeMap)
                    .build();
            OktaGroupsFilter oktaGroupsFilter = OktaGroupsFilter.builder()
                    .integrationIds(integrationIds)
                    .names(getListOrDefault(request.getFilter(), "okta_group_names"))
                    .objectClass(getListOrDefault(request.getFilter(), "okta_object_classes"))
                    .types(getListOrDefault(request.getFilter(), "okta_group_types"))
                    .build();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            aggService.groupJiraIssuesWithOkta(company,
                                    jiraFilter,
                                    oktaGroupsFilter,
                                    ouConfig)));
        });
    }

    private Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> getHygieneCriteriaSpecs(Map<String, Object> filter) {
        try {
            Object idleDays = filter.get(JiraIssuesFilter.EXTRA_CRITERIA.idle.toString());
            Object poorDescCt = filter.get(JiraIssuesFilter.EXTRA_CRITERIA.poor_description.toString());
            Map<JiraIssuesFilter.EXTRA_CRITERIA, Object> specs = new HashMap<>();
            if (idleDays != null)
                specs.put(JiraIssuesFilter.EXTRA_CRITERIA.idle, idleDays);
            if (poorDescCt != null)
                specs.put(JiraIssuesFilter.EXTRA_CRITERIA.poor_description, poorDescCt);
            return specs;
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data.");
        }
    }

    private Long getIngestedAt(String company, IntegrationType type, DefaultListRequest filter)
            throws SQLException {
        Integration integ = integrationService.listByFilter(company,
                null,
                List.of(type.toString()),
                null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(NumberUtils::toInt).collect(Collectors.toList()), List.of(),
                0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    @SuppressWarnings("unchecked")
    private JiraIssuesFilter.JiraIssuesFilterBuilder getJiraIssuesFilterBuilderWithExcludedFilter(
            DefaultListRequest filter) {
        final Map<String, Object> excludedFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        return JiraIssuesFilter.builder()
                .excludeKeys(getListOrDefault(excludedFields, "jira_keys"))
                .excludePriorities(getListOrDefault(excludedFields, "jira_priorities"))
                .excludeStatuses(getListOrDefault(excludedFields, "jira_statuses"))
                .excludeAssignees(getListOrDefault(excludedFields, "jira_assignees"))
                .excludeReporters(getListOrDefault(excludedFields, "jira_reporters"))
                .excludeIssueTypes(getListOrDefault(excludedFields, "jira_issue_types"))
                .excludeFixVersions(getListOrDefault(excludedFields, "jira_fix_versions"))
                .excludeVersions(getListOrDefault(excludedFields, "jira_versions"))
                .excludeIntegrationIds(getListOrDefault(excludedFields, "jira_integration_ids"))
                .excludeProjects(getListOrDefault(excludedFields, "jira_projects"))
                .excludeComponents(getListOrDefault(excludedFields, "jira_components"))
                .excludeLabels(getListOrDefault(excludedFields, "jira_labels"))
                .excludeEpics(getListOrDefault(excludedFields, "jira_epics"))
                .excludeCustomFields(MapUtils.emptyIfNull(
                        (Map<String, Object>) excludedFields.get("custom_fields")));
    }

    public void validateFieldSizeFilter(String company, List<String> integrationIds, Map<String, Map<String, String>> fieldSizes) {
        if(MapUtils.isEmpty(fieldSizes))
            return;
        ArrayList<String> fieldSizeKeys = new ArrayList<>(fieldSizes.keySet());
        try {
            String unknownField = jiraFieldConditionsBuilder.checkFieldsPresent(company, integrationIds, fieldSizeKeys);
            if(unknownField != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unknownField + " is not valid field for size based filter");
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while fetching list of jira fields");
        }
    }
}
