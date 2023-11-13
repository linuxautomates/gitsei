package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/repositories")
@SuppressWarnings("unused")
public class RepositoriesController {
    private GitRepositoryService repositoryService;

    @Autowired
    public RepositoriesController(GitRepositoryService repoService) {
        this.repositoryService = repoService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{repoid}", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<DbRepository>> reposService(@PathVariable("repoid") String repoid,
                                                                     @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(repositoryService.get(company, repoid)
                .orElseThrow(() -> new NotFoundException("Repo id not found."))));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbRepository>>> reposList(@SessionAttribute(name = "company") String company,
                                                                                     @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String integrationId = (String) filter.getFilter().get("integration_id");
            List<String> ids = (List<String>) filter.getFilter().get("ids");
            List<String> orgProductIdsList = getListOrDefault(filter.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            DbListResponse<DbRepository> repositories;
            if (CollectionUtils.isNotEmpty(orgProductIdsSet)) {
                repositories = repositoryService.listByFilter(company, null, null, orgProductIdsSet, filter.getPage(), filter.getPageSize());
            } else if (StringUtils.isNotEmpty(integrationId)) {
                repositories = repositoryService.listByFilter(company, integrationId, ids, orgProductIdsSet, filter.getPage(), filter.getPageSize());
            } else {
                repositories = repositoryService.list(company, filter.getPage(), filter.getPageSize());
            }
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), repositories));
        });
    }
}