package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.internal_api.services.QuestionnaireTemplateService;
import io.levelops.web.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/qtemplates")
public class QuestionnaireTemplateController {

    private final QuestionnaireTemplateService questionnaireTemplateService;

    @Autowired
    public QuestionnaireTemplateController(final QuestionnaireTemplateService questionnaireTemplateService) {
        this.questionnaireTemplateService = questionnaireTemplateService;
    }

    /**
     * POST - Creates a QuestionnaireTemplate object.
     *
     * @param company
     * @param questionnaireTemplate
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> createQuestionnaireTemplate(@PathVariable("company") final String company,
                                                                          @RequestBody final QuestionnaireTemplate questionnaireTemplate) {
        return SpringUtils.deferResponse(() -> {
            QuestionnaireTemplate q = questionnaireTemplateService.create(company, questionnaireTemplate);
            return ResponseEntity.ok().body(Id.from(q.getId()));
        });
    }

    /**
     * GET - Retrieves a questionnare object.
     *
     * @param company
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireTemplate>> questionnaireTemplateDetails(
            @PathVariable("company") final String company,
            @PathVariable("id") final String id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(questionnaireTemplateService.read(company, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire Template with id '" + id + "' not found.'"))));
    }

    /**
     * PUT - Updates a QuestionnaireTemplate object.
     *
     * @param company
     * @param questionnaireTemplate
     * @return
     */
    @RequestMapping(method = RequestMethod.PUT, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> questionnaireTemplateUpdate(@PathVariable("company") final String company,
                                                                          @PathVariable("id") final String id,
                                                                          @RequestBody final QuestionnaireTemplate questionnaireTemplate) {
        return SpringUtils.deferResponse(() -> {
            questionnaireTemplateService.update(company, UUID.fromString(id), questionnaireTemplate);
            return ResponseEntity.ok().body(Id.from(id));
        });
    }

    /**
     * DELETE - Deletes a QuestionnaireTemplate object.
     *
     * @param company
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> questionnaireTemplateDelete(@PathVariable("company") final String company,
                                                                                      @PathVariable("id") final String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                if (!questionnaireTemplateService.delete(company, id)) {
                    return ResponseEntity.ok(DeleteResponse.builder().id(id).success(false)
                            .error("Unable to delete the QuestionnaireTemplate with id=" + id).build());
                }
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(id).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(id).success(true).build());
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkQuestionnaireTemplateDelete(@PathVariable("company") final String company,
                                                                                              @RequestBody final List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            try {
                questionnaireTemplateService.bulkDelete(company, ids);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<QuestionnaireTemplate>>> questionnaireTemplateList(
            @PathVariable("company") final String company,
            @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<QuestionnaireTemplate> results = questionnaireTemplateService.readList(
                    company, filter.getPage(), filter.getPageSize(), filter.getFilter());
            if (results == null || results.getCount() < 1) {
                return ResponseEntity.ok(PaginatedResponse.of(
                        filter.getPage(), filter.getPageSize(), 0, Collections.emptyList()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(),
                    filter.getPageSize(),
                    results.getTotalCount(),
                    results.getRecords()));
        });
    }
}