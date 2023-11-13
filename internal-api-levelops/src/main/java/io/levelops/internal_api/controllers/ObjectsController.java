package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.automation_rules.ObjectTypeDTO;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/objects")
public class ObjectsController {
    private static final Map<ObjectType, List<String>> MAP = Map.of(
            ObjectType.SCM_PULL_REQUEST, List.of("pr_number","created_at","closed_at","updated_at","merged_at","state","title","requester","branch","repo","is_locked","body","products","levelops_id","object_type","patches"),
            ObjectType.GIT_PULL_REQUEST, List.of("pr_number","created_at","closed_at","updated_at","merged_at","state","title","requester","branch","repo","is_locked","body","products","levelops_id","object_type","patches"),
            ObjectType.JIRA_ISSUE, List.of("title", "author", "body", "comments")

    );
    @RequestMapping(method = RequestMethod.GET, value = "/{type}/fields", produces = "application/json")
    public DeferredResult<ResponseEntity<List<String>>> getFieldsForObjectType(@PathVariable("company") String company,
                                                                               @PathVariable("type") String type) {
        return SpringUtils.deferResponse(() -> {
            ObjectType objectType = Optional.ofNullable(ObjectType.fromString(type))
                    .orElseThrow(() -> new NotFoundException("Could not find Object with type=" + type));
            return ResponseEntity.ok(MAP.getOrDefault(objectType, Collections.emptyList()));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> listObjectTypes(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> objectTypes = Arrays.asList(ObjectType.values()).stream().filter(x -> !Boolean.TRUE.equals(x.getDisabled())).map(x -> x.toString()).collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), objectTypes));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list_details", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ObjectTypeDTO>>> listObjectTypeDetails(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<ObjectTypeDTO> objectTypes = Arrays.asList(ObjectType.values()).stream()
                    .filter(x -> !Boolean.TRUE.equals(x.getDisabled()))
                    .map(x -> ObjectTypeDTO.builder()
                            .id(x.toString())
                            .name(x.getName())
                            .fields(MAP.getOrDefault(x, Collections.emptyList()))
                            .build()
                    )
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), objectTypes));
        });
    }
}
