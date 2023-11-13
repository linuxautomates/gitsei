package io.levelops.api.controllers.issue_mgmt;

import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController("issueMgmtWorkItemsFieldsController")
@Log4j2
@RequestMapping("/v1/issue_mgmt/workitems_fields")
@SuppressWarnings("unused")
public class WorkItemsFieldsController {

    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final OrgUnitHelper ouHelper;

    @Autowired
    public WorkItemsFieldsController(WorkItemFieldsMetaService workItemFieldsMetaService, final OrgUnitHelper ouHelper) {
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.ouHelper = ouHelper;
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbWorkItemField>>> listFields(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/attribute/values' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            List<String> fieldKeys = getListOrDefault(request.getFilter(), "field_keys");
            List<String> fieldTypes = getListOrDefault(request.getFilter(), "field_types");
            String exactName = (String) request.getFilter().getOrDefault("name", null);
            String partialName = (String) ((Map<String, Object>) request.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()))
                    .getOrDefault("name", null);
            DbListResponse<DbWorkItemField> fields = workItemFieldsMetaService.listByFilter(
                    company, integrationIds, null, exactName, partialName,
                    fieldKeys, fieldTypes, null, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), fields));
        });
    }
}
