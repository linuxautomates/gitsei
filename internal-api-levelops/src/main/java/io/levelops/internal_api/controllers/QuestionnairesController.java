package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireListItemDTO;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.internal_api.services.QuestionnaireService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/questionnaires")
@Log4j2
public class QuestionnairesController {

    private final QuestionnaireService questionnaireService;
    private final EventsClient eventsClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuestionnairesController(QuestionnaireService questionnaireService, final EventsClient eventsClient, ObjectMapper objectMapper) {
        this.questionnaireService = questionnaireService;
        this.eventsClient = eventsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * POST - Creates a questionnaire object.
     *
     * @param company
     * @param questionnaire
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> createQuestionnaire(@RequestBody final QuestionnaireDTO questionnaire,
                                                                  @PathVariable("company") final String company) {
        return SpringUtils.deferResponse(() -> {
            var id = questionnaireService.createQuestionnaire(company, questionnaire);
            try {
                eventsClient.emitEvent(
                        company,
                        EventType.ASSESSMENT_CREATED,
                        Map.of(
                                "id", id,
                                "tag_ids", questionnaire.getTagIds() != null ? questionnaire.getTagIds() : List.of(),
                                "main", questionnaire.getMain() != null ? questionnaire.getMain() : false,
                                "project_id", questionnaire.getProductId() != null ? questionnaire.getProductId() : "",
                                "priority", questionnaire.getPriority() != null ? questionnaire.getPriority() : "",
                                "ticket_id", questionnaire.getTicketId() != null ? questionnaire.getTicketId() : "",
                                "workitem_id", questionnaire.getWorkItemId(),
                                "sender_email", questionnaire.getSenderEmail() != null ? questionnaire.getSenderEmail() : "",
                                "total_questions", questionnaire.getTotalQuestions()
                        ));
            } catch (EventsClientException e) {
                log.error("Failed to emit the event for assessment created: assessment id={}", id, e);
            }
            return ResponseEntity.ok(Id.from(id));
        });
    }

    /**
     * GET - Retrieves a questionnaire object.
     *
     * @param company
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireDTO>> questionnaireDetails(
            @PathVariable("company") final String company,
            @PathVariable("qnid") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                questionnaireService.questionnaireDetails(company, id)));
    }

    /**
     * PUT - Updates a questionnaire object.
     *
     * @param company
     * @param questionnaire
     * @return
     */
    @RequestMapping(method = RequestMethod.PUT, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireDTO>> questionnaireUpdate(@PathVariable("company") final String company,
                                                                                @PathVariable("qnid") final UUID id,
                                                                                @RequestParam("submitter") final String submitter,
                                                                                @RequestBody final QuestionnaireDTO questionnaire) {
        return SpringUtils.deferResponse(() -> {
            if ((questionnaire.getId() != null) && (!id.toString().equals(questionnaire.getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Id in path param & request body do not match! ID path param : "
                                + id + " Id Request Body : " + questionnaire.getId());
            }
            QuestionnaireDTO sanitized = questionnaire.toBuilder().id(id.toString()).build();
            String qId = questionnaireService.questionnaireUpdate(
                    company, submitter, sanitized);
            try {
                eventsClient.emitEvent(
                        company,
                        questionnaire.getCompleted() ? EventType.ASSESSMENT_SUBMITTED : EventType.ASSESSMENT_UPDATED,
                        Map.of(
                                "id", id,
                                "tags", questionnaire.getTagIds() != null ? questionnaire.getTagIds() : List.of(),
                                "main", questionnaire.getMain() != null ? questionnaire.getMain() : "",
                                "products", questionnaire.getProductId() != null ? questionnaire.getProductId() : "",
                                "priority", questionnaire.getPriority() != null ? questionnaire.getPriority() : "",
                                "ticket", questionnaire.getTicketId() != null ? questionnaire.getTicketId() : "",
                                "workitem", questionnaire.getWorkItemId(),
                                "submitter_email", submitter != null ? submitter : "",
                                "total_questions_answered", questionnaire.getAnsweredQuestions(),
                                "total_questions", questionnaire.getTotalQuestions()
                        ));
            } catch (EventsClientException e) {
                log.error("Failed to emit the event for assessment submitted: assessment id={}", id, e);
            }
            return ResponseEntity.ok().body(questionnaireService.questionnaireDetails(company, UUID.fromString(qId)));
        });
    }

    /**
     * DELETE - Deletes a questionnaire object.
     *
     * @param company
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> questionnaireDelete(@PathVariable("company") final String company,
                                                                              @PathVariable("qnid") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(internalDeleteQuestionnaire(company, id.toString())));
    }

    private DeleteResponse internalDeleteQuestionnaire(String company, String id) {
        try {
            questionnaireService.questionnaireDelete(company, id);
        } catch (Exception e) {
            return DeleteResponse.builder().id(id).success(false).error(e.getMessage()).build();
        }
        return DeleteResponse.builder().id(id).success(true).build();
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> questionnaireBulkDelete(@PathVariable("company") final String company,
                                                                                      @RequestBody final List<UUID> ids) {
        List<DeleteResponse> responses = new ArrayList<>();
        return SpringUtils.deferResponse(() -> {
            ids.forEach(id -> responses.add(internalDeleteQuestionnaire(company, id.toString())));
            return ResponseEntity.ok(BulkDeleteResponse.of(responses));
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<QuestionnaireListItemDTO>>> questionnaireList(
            @PathVariable("company") final String company,
            @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<QuestionnaireListItemDTO> response =
                    questionnaireService.questionnaireList(
                            company,
                            filter,
                            filter.getFilterValue("target_email", String.class).orElse(null)
                    );
            return ResponseEntity.ok(response);
        });
    }

    @PostMapping("/aggregate")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> stackedAggregate(@PathVariable("company") String company,
                                                                                                   @RequestParam("calculation") String calculation,
                                                                                                   @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            QuestionnaireAggFilter questionnaireFilter = (request.getFilter() != null)
                    ? objectMapper.convertValue(request.getFilter(), QuestionnaireAggFilter.class)
                    : QuestionnaireAggFilter.builder().build();
            var stacks = ListUtils.emptyIfNull(request.getStacks()).stream()
                    .map(QuestionnaireAggFilter.Distinct::valueOf)
                    .collect(Collectors.toList());
            questionnaireFilter = questionnaireFilter.toBuilder()
                    .across(QuestionnaireAggFilter.Distinct.fromString(request.getAcross()))
                    .calculation(QuestionnaireAggFilter.Calculation.fromString(calculation))
                    .build();
            return ResponseEntity.ok(questionnaireService.stackedAggregate(company, questionnaireFilter, stacks, request));
        });
    }

    @PostMapping("/aggregate_paginated")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> stackedAggregatePaginated(@PathVariable("company") String company,
                                                                                                            @RequestParam("calculation") String calculation,
                                                                                                            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            QuestionnaireAggFilter questionnaireFilter = (request.getFilter() != null)
                    ? objectMapper.convertValue(request.getFilter(), QuestionnaireAggFilter.class)
                    : QuestionnaireAggFilter.builder().build();
            var stacks = ListUtils.emptyIfNull(request.getStacks()).stream()
                    .map(QuestionnaireAggFilter.Distinct::valueOf)
                    .collect(Collectors.toList());
            questionnaireFilter = questionnaireFilter.toBuilder()
                    .across(QuestionnaireAggFilter.Distinct.fromString(request.getAcross()))
                    .calculation(QuestionnaireAggFilter.Calculation.fromString(calculation))
                    .build();
            return ResponseEntity.ok(questionnaireService.stackedAggregateResponseTime(company, questionnaireFilter, stacks, request));
        });
    }
}