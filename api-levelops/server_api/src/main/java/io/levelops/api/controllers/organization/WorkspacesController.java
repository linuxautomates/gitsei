package io.levelops.api.controllers.organization;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.Workspace;
import io.levelops.commons.databases.services.organization.WorkspaceDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Log4j2
// @RestController
// @RequestMapping("/v1/org/workspaces")
// @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class WorkspacesController {
    private final WorkspaceDatabaseService workspaceService;

    // @Autowired
    public WorkspacesController(final WorkspaceDatabaseService workspaceService){
        this.workspaceService = workspaceService;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Workspace>> getWorkspace(@SessionAttribute(name = "company") final String company, @PathVariable UUID id){
        return SpringUtils.deferResponse(() -> {
            var workspace = workspaceService.get(company, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find Workspace with id: " + id));
            return  ResponseEntity.ok(workspace);
        });
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> createWorkspace(@SessionAttribute(name = "company") final String company, @RequestBody Workspace workspace) {
        return SpringUtils.deferResponse(() -> {
            var workspaceId = workspaceService.insert(company, workspace);
            if (StringUtils.isBlank(workspaceId)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to create the workspace, please check the request or contact support.");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(workspaceId);
        });
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Workspace>> updateWorkspace(@SessionAttribute(name = "company") final String company, @PathVariable UUID id, @RequestBody Workspace workspace) {
        return SpringUtils.deferResponse(() -> {
            var result = workspaceService.update(company, workspace.toBuilder().id(id).build());
            if (result) {
                var workspaceUpdated = workspaceService.get(company, id);
                return ResponseEntity.ok(workspaceUpdated.get());
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "");
        });
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Workspace>> deleteWorkspace(@SessionAttribute(name = "company") final String company, @PathVariable UUID id){
        return SpringUtils.deferResponse(() -> {
            var workspace = workspaceService.get(company, id);
            if (workspace.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to find the workspace with id: " + id);
            }
            if (!workspaceService.delete(company, id)){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete the workspace with id: " + id);
            }
            return ResponseEntity.ok(workspace.get());
        });

    }

    @PostMapping(path="/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Workspace>>> listWorkspaces(@SessionAttribute(name = "company") final String company, @RequestBody DefaultListRequest request){
        return SpringUtils.deferResponse(() -> {
            var filters = QueryFilter.fromRequestFilters(request.getFilter());
            var integrationTypes = request.<String>getFilterValueAsSet("integration_type").orElseGet(() -> Set.of());
            var integrationIds = request.<Integer>getFilterValueAsSet("integration_id").orElseGet(() -> Set.of());
            var workspacesIds = request.<UUID>getFilterValueAsSet("workspace_id").orElseGet(() -> Set.of());
            var categories = request.getFilterValueAsSet("category").orElseGet(() -> Set.of());
            var name = (filters.getPartialMatches()!=null && filters.getPartialMatches().get("name")!=null)?((Map<String,String>)filters.getPartialMatches().get("name")).get("starts"):null;
            // TODO: implement search by category, integration type, integration id
            var results = workspaceService.listByFilter(company, name, null, integrationIds, null, null, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), results));
        });

    }
}
