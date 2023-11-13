package io.levelops.controlplane.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.controlplane.models.DbJob;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum JobRetryingStrategy {
    DEFAULT(null, 5, attemptCount -> (attemptCount + 1) * 60), // 1 extra min per attempt
    GITHUB("GithubIterativeScanController", 24, attemptCount -> 3660), // wait 1h 1min regardless of attempt
    ADO("AzureDevopsIterativeScanController", 24, attemptCount -> 3660); // wait 1h 1min regardless of attempt

    private final String controllerName;
    int attemptMax;
    Function<Integer, Integer> waitStrategy; // attemptCount -> seconds

    JobRetryingStrategy(String controllerName, int attemptMax, Function<Integer, Integer> waitStrategy) {
        this.controllerName = controllerName;
        this.attemptMax = attemptMax;
        this.waitStrategy = waitStrategy;
    }

    public static List<String> getAllControllerNames() {
        return StreamUtils.toStream(values())
                .map(JobRetryingStrategy::getControllerName)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    public static JobRetryingStrategy forJob(DbJob job) {
        String controllerName = StringUtils.defaultString(job.getControllerName());
        for (JobRetryingStrategy strategy : values()) {
            if (controllerName.equals(strategy.getControllerName())) {
                return strategy;
            }
        }
        return DEFAULT;
    }
}
