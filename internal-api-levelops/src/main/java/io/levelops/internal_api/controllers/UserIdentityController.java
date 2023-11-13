package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/user_identities")
public class UserIdentityController {

    private final UserIdentityService userIdentityService;

    @Autowired
    public UserIdentityController(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }


    @RequestMapping(method = RequestMethod.POST, value = "/list", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmUser>>> usersList(@PathVariable(name = "company") String company
                                                                                ,@RequestBody final DefaultListRequest filter) {
        UserIdentitiesFilter userIdentitiesFilter = UserIdentitiesFilter.fromDefaultListRequest(company,
                filter, UserIdentityService.USERS_PARTIAL_MATCH_COLUMNS);
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                    userIdentityService.list(company, userIdentitiesFilter,
                            filter.getPage(), filter.getPageSize())));
        });
    }

}
