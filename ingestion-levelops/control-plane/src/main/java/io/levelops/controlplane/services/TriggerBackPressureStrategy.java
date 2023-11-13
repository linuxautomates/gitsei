package io.levelops.controlplane.services;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum TriggerBackPressureStrategy {
    DEFAULT(null, JobRetryingStrategy.DEFAULT.getAttemptMax()),
    GITHUB("github", JobRetryingStrategy.GITHUB.getAttemptMax()),
    ADOGITHUB("azure_devops", JobRetryingStrategy.ADO.getAttemptMax());

    private final String triggerType;
    private final int failedJobsMaxAttempt;

    TriggerBackPressureStrategy(String triggerType, int failedJobsMaxAttempt) {
        this.triggerType = triggerType;
        this.failedJobsMaxAttempt = failedJobsMaxAttempt;
    }

    public static TriggerBackPressureStrategy forTriggerType(String type) {
        if (StringUtils.isBlank(type)) {
            return DEFAULT;
        }
        for (TriggerBackPressureStrategy strategy : values()) {
            if (type.equalsIgnoreCase(strategy.getTriggerType())) {
                return strategy;
            }
        }
        return DEFAULT;
    }
}
