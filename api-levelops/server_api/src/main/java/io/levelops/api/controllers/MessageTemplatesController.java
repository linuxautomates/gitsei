package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/message_templates")
@SuppressWarnings("unused")
public class MessageTemplatesController {
    private ObjectMapper objectMapper;
    private MsgTemplateService msgTemplateService;

    @Autowired
    public MessageTemplatesController(@Qualifier("custom") ObjectMapper objectMapper, MsgTemplateService service) {
        this.objectMapper = objectMapper;
        this.msgTemplateService = service;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createTemplate(@SessionAttribute(name = "company") String company,
                                                                              @RequestBody MessageTemplate messageTemplate) {
        return SpringUtils.deferResponse(() -> {
            String result = msgTemplateService.insert(company, messageTemplate);
            return ResponseEntity.accepted().body(Map.of("id", result));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{templateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> updateTemplate(@SessionAttribute(name = "company") String company,
                                                               @PathVariable("templateid") String templateId,
                                                               @RequestBody MessageTemplate template) {
        if (StringUtils.isEmpty(template.getId())) {
            template = MessageTemplate.builder()
                    .name(template.getName())
                    .emailSubject(template.getEmailSubject())
                    .botName(template.getBotName())
                    .id(templateId)
                    .message(template.getMessage())
                    .type(template.getType())
                    .defaultTemplate(template.isDefaultTemplate())
                    .eventType(template.getEventType())
                    .build();
        }
        final MessageTemplate templateToUpdate = template;
        return SpringUtils.deferResponse(() -> {
            msgTemplateService.update(company, templateToUpdate);
            return ResponseEntity.accepted().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{templateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteTemplate(@SessionAttribute(name = "company") String company,
                                                                         @PathVariable("templateid") String templateId) {
        return SpringUtils.deferResponse(() -> {
            try {
                msgTemplateService.delete(company, templateId);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(templateId).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(templateId).success(true).build());
        });

    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteTemplates(@SessionAttribute(name = "company") String company,
                                                                              @RequestBody List<String> templateIds) {
        return SpringUtils.deferResponse(() -> {
            List<String> filteredIds = templateIds.stream()
                    .map(NumberUtils::toInt)
                    .map(Number::toString)
                    .collect(Collectors.toList());
            try {
                msgTemplateService.bulkDelete(company, filteredIds);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(templateIds, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(templateIds, false, e.getMessage()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{templateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<MessageTemplate>> getTemplate(@SessionAttribute(name = "company") String company,
                                                                       @PathVariable("templateid") String templateId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(msgTemplateService.get(company, templateId)
                .orElseThrow(() -> new NotFoundException("Message template not found"))));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<MessageTemplate>>> listTemplates(@SessionAttribute(name = "company") String company,
                                                                                            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String partialName = (filter.getFilter().get("partial") != null) ?
                    (String) ((Map<String, Object>) filter.getFilter().get("partial")).get("name")
                    : null;
            DbListResponse<MessageTemplate> templates = msgTemplateService.listByFilter(
                    company,
                    partialName,
                    (String) filter.getFilter().get("type"),
                    filter.getPage(),
                    filter.getPageSize(), null, null, null);
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(),
                    filter.getPageSize(), templates));
        });
    }

}
