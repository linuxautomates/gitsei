package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Integration;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PreflightCheckResults.PreflightCheckResultsBuilder.class)
public class PreflightCheckResults {

    /**
     * If the preflight check was successful as a whole.
     */
    @JsonProperty("success")
    boolean success;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration")
    Integration integration;

    /**
     * Exception if it was thrown outside of any single sub-check.
     */
    @JsonProperty("exception")
    String exception;

    /**
     * Individual checks composing the Preflight check.
     */
    @Singular
    @JsonProperty("checks")
    List<PreflightCheckResult> checks;

    public static class PreflightCheckResultsBuilder {

        private boolean allChecksMustPass = false;
        private boolean success = true;

        public PreflightCheckResultsBuilder allChecksMustPass() {
            allChecksMustPass = true;
            return this;
        }

        public PreflightCheckResults build() {
            if (allChecksMustPass) {
                if (checks != null && checks.stream()
                        .map(PreflightCheckResult::getSuccess)
                        .anyMatch(bool -> !Boolean.TRUE.equals(bool))) {
                    this.success(false);
                }
            }
            return new PreflightCheckResults(success, tenantId, integration, exception, checks);
        }

    }

}
