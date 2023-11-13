package io.levelops.aggregations.controllers;

import io.levelops.aggregations.models.messages.JenkinsLogTriagingMessage;
import io.levelops.aggregations.services.TriageLocalService;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Log4j2
@Service
public class RulesController implements AggregationsController<JenkinsLogTriagingMessage> {
    private final String subscriptionName;
    private final TriageLocalService triageService;

    public RulesController(@Value("${TRIAGE_RULES_SUB}") String subscriptionName,
                           final TriageLocalService triageService) {
        this.subscriptionName = subscriptionName;
        this.triageService = triageService;
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.TRIAGE_RULE;
    }

    @Override
    public Class<JenkinsLogTriagingMessage> getMessageType() {
        return JenkinsLogTriagingMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    @Override
    @Async("rulesTaskExecutor")
    public void doTask(JenkinsLogTriagingMessage task) {
        try {
            LoggingUtils.setupThreadLocalContext(task.getMessageId(), task.getCustomer(), "rules", null);
            log.info("[{}] Starting jenkins logs analysis for triage: {}", task.getCompany(), task.getLogLocation());
            triageService.analyzeJenkinsGCSLogs(
                    task.getCompany(),
                    task.getInstanceId().toString(),
                    task.getInstanceName(),
                    task.getJobId().toString(),
                    task.getJobRunId().toString(),
                    task.getJobName(),
                    task.getJobStatus(),
                    task.getStageId() != null ? task.getStageId().toString() : null,
                    task.getStepId() != null ? task.getStepId().toString() : null,
                    task.getLogLocation(),
                    task.getLogBucket(),
                    task.getUrl());
        } catch (Throwable e) {
            log.error("Unable to successfully analyze the logs for: {}", task, e);
        }
    }
}