package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.tags.models.FindTagsByValueRequest;
import io.levelops.web.util.SpringUtils;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/tags")
public class TagsController {

    private final TagsService tagsService;

    @Autowired
    public TagsController(TagsService tagsService) {
        this.tagsService = tagsService;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Tag>> getById(@PathVariable("company") String company,
                                                       @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.of(tagsService.get(company, id)));
    }

    @RequestMapping(method = RequestMethod.POST, path = "/find", produces = "application/json")
    public DeferredResult<ResponseEntity<List<String>>> findByValue(@PathVariable("company") String company,
                                                                    @RequestBody FindTagsByValueRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<String> tagIds;
            if (BooleanUtils.isTrue(request.getCreateIfMissing())) {
                tagIds = ListUtils.emptyIfNull(tagsService.forceGetTagIds(company, request.getValues()));
            } else {
                tagIds = ListUtils.emptyIfNull(tagsService.findTagsByValues(company, request.getValues())).stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok(tagIds);
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Tag>>> list(@PathVariable("company") String company,
                                                                       @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            @SuppressWarnings("unchecked")
            DbListResponse<Tag> results = tagsService.listByFilter(company,
                    (List<String>) filter.getFilterValue("tag_ids", List.class).orElse(null),
                    filter.getFilterValueAsMap("partial").map(m -> (String) m.get("name")).orElse(null),
                    filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    results.getTotalCount(),
                    ListUtils.emptyIfNull(results.getRecords())));
        });
    }
}
