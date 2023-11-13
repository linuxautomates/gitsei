package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/ticket_templates")
public class TicketTemplateController {

    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_TICKET_TEMPLATE";
    private final DefaultConfigService defaultConfigService;
    private final TicketTemplateDBService ticketTemplateDBService;

    @Autowired
    public TicketTemplateController(DefaultConfigService defaultConfigService,
                                    TicketTemplateDBService ticketTemplateDBService) {
        this.defaultConfigService = defaultConfigService;
        this.ticketTemplateDBService = ticketTemplateDBService;
    }

    @PostMapping("/list")
    @SuppressWarnings("unchecked")
    public DeferredResult<ResponseEntity<PaginatedResponse<TicketTemplate>>> ticketTemplateList(
            @PathVariable("company") final String company,
            @RequestBody final DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            if (listRequest.getFilterValue("default", Boolean.class).orElse(false)) {
                String resultId = defaultConfigService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
                List<TicketTemplate> dto = new ArrayList<>();
                if (StringUtils.isNotEmpty(resultId)) {
                    ticketTemplateDBService.get(company, resultId)
                            .ifPresent(template -> dto.add(template.toBuilder()
                                    .isDefault(true)
                                    .build()));
                }
                return ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                listRequest.getPage(),
                                listRequest.getPageSize(),
                                dto.size(),
                                dto));
            }

            Map<String, Integer> updateRange = listRequest.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;
            
            DbListResponse<TicketTemplate> ticketTemplates = ticketTemplateDBService.listByFilters(
                    company,
                    (String) listRequest.getFilterValue("partial", Map.class)
                            .orElse(Collections.emptyMap())
                            .get("name"),
                    updatedAtStart,
                    updatedAtEnd,
                    listRequest.getPage(),
                    listRequest.getPageSize());
            String defaultId = defaultConfigService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
            PaginatedResponse<TicketTemplate> paginatedResponse = PaginatedResponse.of(
                    listRequest.getPage(),
                    listRequest.getPageSize(),
                    ticketTemplates.getTotalCount(),
                    ticketTemplates.getCount() == 0 ?
                            Collections.emptyList() : ticketTemplates.getRecords()
                            .stream()
                            .map(item -> item.toBuilder()
                                    .isDefault(defaultId.equalsIgnoreCase(item.getId()))
                                    .build())
                            .collect(Collectors.toList()));
            return ResponseEntity.ok().body(paginatedResponse);
        });
    }


}
