package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.converters.DefaultListRequestUtils;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.TestRailsCaseFieldDTO;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TestRailsCaseFieldDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/testrails_tests/custom_case_fields")
public class TestRailsCaseFieldController {

    private final TestRailsCaseFieldDatabaseService testRailsCaseFieldService;
    private final IntegrationService integService;
    private final IntegrationTrackingService integrationTrackingService;
    private final OrgUnitHelper orgUnitHelper;

    public TestRailsCaseFieldController(TestRailsCaseFieldDatabaseService testRailsCaseFieldService,
                                        IntegrationService integService, IntegrationTrackingService integrationTrackingService,
                                        final OrgUnitHelper orgUnitHelper) {
        this.testRailsCaseFieldService = testRailsCaseFieldService;
        this.integService = integService;
        this.integrationTrackingService = integrationTrackingService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<TestRailsCaseFieldDTO>>> fieldsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {

        return SpringUtils.deferResponse(() -> {
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/testrails_tests/custom_case_fields/list' for the request: {}", company, originalRequest, e);
            }

            String integrationId = (String) request.getFilter().get("integration_id");
            List<String> integrationIds;
            if (Objects.nonNull(integrationId)) {
                integrationIds = List.of(integrationId);
            } else {
                integrationIds = DefaultListRequestUtils.getListOrDefault(request.getFilter(), "integration_ids");
            }
            DbListResponse<DbTestRailsCaseField> fields = testRailsCaseFieldService.listByFilter(
                    company, TestRailsCaseFieldFilter.builder().integrationIds(integrationIds).needAssignedFieldsOnly(true).build(), request.getPage(), request.getPageSize());
            DbListResponse<TestRailsCaseFieldDTO> caseFields = DbListResponse.of(fields.getRecords().stream().map(TestRailsCaseFieldDTO::fromDbCaseField).collect(Collectors.toList()), fields.getTotalCount());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), caseFields));
        });
    }
}
