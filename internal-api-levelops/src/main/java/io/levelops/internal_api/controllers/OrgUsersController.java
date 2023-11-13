package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.organization.OrgUserDTO;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/org/users")
public class OrgUsersController {

    private final OrgUsersHelper usersHelper;
    private final OrgUsersDatabaseService usersService;

    @Autowired
    public OrgUsersController(OrgUsersHelper usersHelper, OrgUsersDatabaseService usersService) {
        this.usersHelper = usersHelper;
        this.usersService = usersService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUsers(@PathVariable("company") final String company, @RequestBody Set<OrgUserDTO> users){
        return SpringUtils.deferResponse(() -> {
            var ids = usersHelper.insertNewVersionUsers(company, users.stream()
                    .map(dto -> map(dto)));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", ids, "errors", ""));
        });
    }
    private DBOrgUser map(final OrgUserDTO dto) {
        var builder = DBOrgUser.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .ids(dto.getIntegrationUserIds().stream()
                        .map(id -> DBOrgUser.LoginId.builder()
                                .integrationId(Integer.valueOf(id.getIntegrationId()))
                                .cloudId(id.getUserId())
                                .build())
                        .collect(Collectors.toSet()))
                .customFields(dto.getAdditionalFields());
        if (dto.getId() != null) {
            builder.refId(Integer.valueOf(dto.getId()));
        }
        return builder.build();
    }

    private OrgUserDTO map(final DBOrgUser dbUser){
        return OrgUserDTO.builder()
                .id(String.valueOf(dbUser.getRefId()))
                .orgUUID(dbUser.getId())
                .fullName(dbUser.getFullName())
                .email(dbUser.getEmail())
                .version(dbUser.getVersions().stream().max((v,b) -> v < b ? b : v).get().toString())
                .additionalFields(dbUser.getCustomFields())
                .integrationUserIds(dbUser.getIds().stream()
                        .map(loginId -> OrgUserDTO.IntegrationUserId.builder()
                                .integrationId(String.valueOf(loginId.getIntegrationId()))
                                .userId((String) loginId.getCloudId())
                                .build())
                        .collect(Collectors.toSet()))
                .createdAt(dbUser.getCreatedAt())
                .updatedAt(dbUser.getUpdatedAt())
                .build();
    }

    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> listUsers(@PathVariable(name = "company") final String company,
                                                                         @RequestBody DefaultListRequest request, @RequestParam(name = "version", required = false) Integer version){
        return SpringUtils.deferResponse(() -> {
            QueryFilter filters = QueryFilter.fromRequestFilters(request.getFilter());
            if(version != null) {
                var builder = filters == null ? QueryFilter.builder() : filters.toBuilder();
                filters = builder.strictMatch("version", version).build();
            }
            var listResponse = usersService.filter(company, filters, request.getPage(), request.getPageSize(), true);
            return ResponseEntity.ok(Map.of(
                    "_metadata", Map.of(
                            "next_page", request.getPage() + 1,
                            "has_next", listResponse.getTotalCount() > listResponse.getCount(),
                            "page", request.getPage(),
                            "page_size", request.getPageSize(),
                            "total_count", listResponse.getTotalCount()
                    ),
                    "records", listResponse.getRecords().stream()
                            .map(item -> map(item))
                            .collect(Collectors.toList()),

                    "non_users_count", listResponse.getTotals().getOrDefault("dangling", 0)));
        });
    }
}