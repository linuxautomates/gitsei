package io.levelops.ingestion.engine.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.models.Job;

public class JobConverter {

    public static Job fromEngineJob(IngestionEngine.EngineJob job) {

        return Job.builder()
                .id(job.getId())
                .agentId(job.getAgentId())
                .controllerId(job.getControllerId())
                .controllerName(job.getControllerName())
                .done(job.isDone())
                .cancelled(job.isCanceled())
                .createdAt(job.getCreatedAt())
                .doneAt(job.getDoneAt())
                .query(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), job.getQuery()))
                .exception(ExceptionPrintout.fromThrowable(job.getException()))
                .result(job.getResult())
                .intermediateState(job.getIntermediateState())
                .ingestionFailures(job.getIngestionFailures())
                .build();
    }

}
