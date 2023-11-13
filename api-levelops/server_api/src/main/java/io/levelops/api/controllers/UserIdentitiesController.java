package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/user_identities")
@SuppressWarnings("unused")
public class UserIdentitiesController {
    private final ObjectMapper mapper;
    private final UserIdentityService userIdentityService;
    private final AggCacheService aggCacheService;

    @Autowired
    public UserIdentitiesController(ObjectMapper mapper, UserIdentityService userIdentityService, AggCacheService aggCacheService) {
        this.mapper = mapper;
        this.userIdentityService = userIdentityService;
        this.aggCacheService = aggCacheService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmUser>>> usersList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            UserIdentitiesFilter userIdentitiesFilter = UserIdentitiesFilter.fromDefaultListRequest(company,
                    filter, UserIdentityService.USERS_PARTIAL_MATCH_COLUMNS);
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company, "/user_identities/list",
                            userIdentitiesFilter.generateCacheHash(), userIdentitiesFilter.getIntegrationIds(),
                            mapper, aggCacheService, () -> userIdentityService.list(company, userIdentitiesFilter,
                                    filter.getPage(), filter.getPageSize()))));
        });
    }
}
