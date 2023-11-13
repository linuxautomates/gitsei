package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.ba.TicketCategorizationSchemesService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.exceptions.CallableWithException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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

import java.util.Map;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/v1/ticket_categorization_schemes")
@SuppressWarnings("unused")
public class TicketCategorizationSchemesController {

    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final TicketCategorizationSchemesService ticketCategorizationSchemesService;

    @Autowired
    public TicketCategorizationSchemesController(TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService, TicketCategorizationSchemesService ticketCategorizationSchemesService) {
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        this.ticketCategorizationSchemesService = ticketCategorizationSchemesService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD', 'SUPER_ADMIN')")
    @PostMapping
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@SessionAttribute(name = "company") String company,
                                                                      @RequestBody TicketCategorizationScheme scheme) {
        return SpringUtils.deferResponse(() -> handleUniqueNameConstraintException(
                () -> ResponseEntity.ok(Map.of("id", ticketCategorizationSchemesService.insert(company, scheme)))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/{id}")
    public DeferredResult<ResponseEntity<TicketCategorizationScheme>> getById(@SessionAttribute(name = "company") String company,
                                                                              @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(ticketCategorizationSchemeDatabaseService.get(company, id.toString())
                .orElseThrow(() -> new NotFoundException("Could not find ticket categorization scheme with id=" + id))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/default")
    public DeferredResult<ResponseEntity<TicketCategorizationScheme>> getDefault(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(ticketCategorizationSchemeDatabaseService.getDefaultScheme(company)
                .orElseThrow(() -> new NotFoundException("Could not find default ticket categorization scheme"))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public DeferredResult<ResponseEntity<Map<String, Boolean>>> update(@SessionAttribute(name = "company") String company,
                                                                       @PathVariable("id") UUID id,
                                                                       @RequestBody TicketCategorizationScheme scheme) {
        return SpringUtils.deferResponse(() -> handleUniqueNameConstraintException(() ->
                ResponseEntity.ok(Map.of("changed",
                        ticketCategorizationSchemesService.update(company, id, scheme)
                ))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public DeferredResult<ResponseEntity<Void>> delete(@SessionAttribute(name = "company") String company,
                                                       @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> {
            Boolean deleted = ticketCategorizationSchemeDatabaseService.delete(company, id.toString());
            return BooleanUtils.isNotTrue(deleted) ? ResponseEntity.badRequest().build() : ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/list")
    public DeferredResult<ResponseEntity<DbListResponse<TicketCategorizationScheme>>> list(@SessionAttribute(name = "company") String company,
                                                                                           @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(ticketCategorizationSchemeDatabaseService.filter(request.getPage(), request.getPageSize(), company,
                TicketCategorizationSchemeDatabaseService.TicketCategorizationSchemeFilter.builder()
                        .ids(request.<String>getFilterValueAsList("ids").orElse(null))
                        .partialName(request.getFilterValueAsMap("partial").map(m -> (String) m.get("name")).orElse(null))
                        .defaultScheme(request.getFilterValue("default_scheme", Boolean.class).orElse(null))
                        .build())));
    }

    public static <T> T handleUniqueNameConstraintException(CallableWithException<T, Exception> delegate) throws Exception {
        try {
            return delegate.call();
        } catch (DuplicateKeyException e) {
            if (StringUtils.defaultString(e.getMessage()).contains("ticket_categorization_schemes_name_key")) {
                throw new BadRequestException("A ticket categorization scheme with that name already exists.");
            }
            throw e;
        }
    }
}
