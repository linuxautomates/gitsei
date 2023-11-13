package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tags")
@Log4j2
@SuppressWarnings("unused")
public class TagsController {

    private final TagsService tagsService;

    @Autowired
    public TagsController(final TagsService tagsService) {
        this.tagsService = tagsService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, path = "/{tagid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Tag>> tagDetails(@SessionAttribute(name = "company") final String company,
                                                          @PathVariable(name = "tagid") String tagId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(tagsService.get(company, tagId)
                .orElseThrow(NotFoundException::new)));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createTag(@SessionAttribute(name = "company") String company,
                                                                         @RequestBody Tag tag) {
        return SpringUtils.deferResponse(() -> {
            try {
                return ResponseEntity.ok(Map.of("id", tagsService.insert(company, tag)));
            } catch (Exception e) {
                if (!(e instanceof PSQLException && ((PSQLException) e).getServerErrorMessage()
                        .getMessage().contains("violates unique constraint"))) {
                    throw e;
                } else {
                    throw new BadRequestException("Tag name already exists at this level.");
                }
            }
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{tagid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> tagDelete(@SessionAttribute(name = "company") String company,
                                                          @PathVariable("tagid") String tagId) {
        return SpringUtils.deferResponse(() -> {
            tagsService.delete(company, tagId);
            return ResponseEntity.ok().build();
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{tagid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> tagUpdate(@SessionAttribute(name = "company") String company,
                                                             @PathVariable("tagid") String tagId,
                                                             @RequestBody Tag tag) {
        if (StringUtils.isEmpty(tag.getId())) {
            tag = Tag.builder().id(tagId).name(tag.getName()).build();
        }
        final Tag tagToUpdate = tag;
        return SpringUtils.deferResponse(() -> {
            try {
                return ResponseEntity.ok(tagsService.update(company, tagToUpdate));
            } catch (Exception e) {
                if (!(e instanceof PSQLException && ((PSQLException) e).getServerErrorMessage()
                        .getMessage().contains("violates unique constraint"))) {
                    throw e;
                } else {
                    throw new BadRequestException("Tag name already exists at this level.");
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Tag>>> tagsList(
            @SessionAttribute(name = "company") final String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            final Map<String, Object> partial = MoreObjects.firstNonNull(
                    (Map<String, Object>) filter.getFilter().get("partial"), Collections.emptyMap());
            final String name = (String) partial.getOrDefault("name", "");
            final List<String> tagIds = (List<String>) filter.getFilter().get("tag_ids");
            DbListResponse<Tag> results = tagsService.listByFilter(company, tagIds,
                    name, filter.getPage(), filter.getPageSize());
            if (results == null || results.getCount() < 1) {
                return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        0, Collections.emptyList()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    results.getTotalCount(),
                    results.getRecords()));
        });
    }
}