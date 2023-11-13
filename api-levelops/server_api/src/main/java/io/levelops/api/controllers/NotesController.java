package io.levelops.api.controllers;

import com.google.common.base.Strings;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.WorkItemNote;
import io.levelops.commons.databases.services.WorkItemNotesService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notes")
@Log4j2
@SuppressWarnings("unused")
public class NotesController {

    private final WorkItemNotesService notesService;

    @Autowired
    public NotesController(final WorkItemNotesService notesService) {
        this.notesService = notesService;
    }

    /**
     * POST - Creates a notes object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createNotes(@SessionAttribute("company") final String company,
                                                                           @RequestBody final WorkItemNote note) {
        return SpringUtils.deferResponse(() -> {
            var id = notesService.insert(company, note);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * GET - Retrieves a note object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItemNote>> notesDetails(@SessionAttribute("company") final String company, @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(notesService.get(company, id.toString())
                .orElseThrow(() -> new NotFoundException("Note not found"))
                .toBuilder()
                .id(id.toString()).build()));
    }

    // List
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<WorkItemNote>>> notesList(@SessionAttribute("company") final String company,
                                                                                     @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            var workItemId = (String) filter.getFilter().getOrDefault("work_item_id", "");
            var results = Strings.isNullOrEmpty(workItemId)
                    ? notesService.list(company, filter.getPage(), filter.getPageSize())
                    : notesService.list(company, workItemId, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), results));
        });
    }
}