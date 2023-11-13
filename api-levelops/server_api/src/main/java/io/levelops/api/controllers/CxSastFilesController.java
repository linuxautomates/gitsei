package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.filters.CxSastIssueFilter;
import io.levelops.commons.databases.services.CxSastAggService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/cxsast_files")
@SuppressWarnings("unused")
public class CxSastFilesController {
    private final CxSastAggService aggService;
    private final ConfigTableHelper configTableHelper;
    private final CxSastIssueFilterParser filterParser;

    @Autowired
    public CxSastFilesController(CxSastAggService aggService, ConfigTableHelper configTableHelper,
                                 CxSastIssueFilterParser filterParser) {
        this.aggService = aggService;
        this.configTableHelper = configTableHelper;
        this.filterParser = filterParser;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbCxSastIssue>>> filesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            CxSastIssueFilter cxSastIssueFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                cxSastIssueFilter = filterParser.createFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null,false);
            } else {
                cxSastIssueFilter = filterParser.createFilter(filter, null, null, false);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            aggService.listFiles(company,
                                    cxSastIssueFilter,
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())),
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }
}
