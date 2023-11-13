package io.levelops.ingestion.agent.controllers;

import io.levelops.commons.models.AgentResponse;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.agent.model.jobs.JobView;
import io.levelops.ingestion.agent.services.ResponseFactory;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.engine.EngineEntity;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.exceptions.TooManyRequestsException;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Log4j2
@RestController
@RequestMapping("/v1/jobs")
public class JobController {

    private final IngestionEngine ingestionEngine;
    private ResponseFactory responseFactory;
    private final String agentId;

    @Autowired
    public JobController(IngestionEngine ingestionEngine,
                         ResponseFactory responseFactory,
                         @Qualifier("agentId") String agentId) {
        this.ingestionEngine = ingestionEngine;
        this.responseFactory = responseFactory;
        this.agentId = agentId;
    }


    @GetMapping
    public AgentResponse<ListResponse<JobView>> getJobs(
            @RequestParam(name = "done", required = false) Optional<Boolean> done) {
        return responseFactory.build(ListResponse.<JobView>builder()
                .records(ingestionEngine.getJobs().stream()
                        .filter(job -> done.isEmpty() || done.get().equals(job.isDone()))
                        .map(JobView::fromEngineJob)
                        .sorted(Comparator.comparing((JobView j) -> j.getJob().getCreatedAt()).reversed())
                        .collect(Collectors.toList()))
                .build());
    }

    @GetMapping("/{id}")
    public AgentResponse<JobView> getJob(@PathVariable String id) throws NotFoundException {
        return responseFactory.build(ingestionEngine.getJobById(id)
                .map(JobView::fromEngineJob)
                .orElseThrow(() -> new NotFoundException("Could not find job with id=" + id)));
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteJob(@PathVariable String id) throws NotFoundException {
        ingestionEngine.getJobById(id)
                .map(ingestionEngine::cancelJob)
                .orElseThrow(() -> new NotFoundException("Could not find job with id=" + id));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void clearJobs() {
        ingestionEngine.clearJobs();
    }

    @PostMapping
    public AgentResponse<JobView> submitJob(@RequestBody CreateJobRequest request) throws BadRequestException, TooManyRequestsException {

        final String controllerField;
        Optional<EngineEntity> entity;
        if (Strings.isNotEmpty(request.getControllerName())) {
            entity = ingestionEngine.getEntityByName(request.getControllerName());
            controllerField = request.getControllerName();
        } else {
            throw new BadRequestException("Invalid Create Job request: controller_name required");
        }

        @SuppressWarnings("unchecked")
        DataController<? extends DataQuery> controller = entity
                .map(EngineEntity::getIngestionComponent)
                .map(DataController.class::cast)
                .orElseThrow(() -> new BadRequestException("Controller does not exist: " + controllerField));

        log.info("▶︎ {}", request.toString().replaceAll("\n", "\\n"));

        DataQuery dataQuery = controller.parseQuery(request.getQuery());

        JobContext jobContext = JobContext.builder()
                .jobId(request.getJobId())
                .tenantId(request.getTenantId())
                .integrationId(request.getIntegrationId())
                .intermediateState(request.getIntermediateState())
                .attemptCount(request.getAttemptCount())
                .build();

        JobView job = ingestionEngine.submitJob(controller, dataQuery, agentId, jobContext, request.getCallbackUrl(), request.getJobId())
                .map(JobView::fromEngineJob)
                .orElseThrow(() -> new TooManyRequestsException("Cannot submit job at this point" + request));

        return responseFactory.build(job);
    }

}
