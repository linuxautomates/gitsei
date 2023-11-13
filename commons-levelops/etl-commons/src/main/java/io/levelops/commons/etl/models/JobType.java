package io.levelops.commons.etl.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JobType {
    /**
     * Jobs that process ingestion results for a particular integration type.
     * There is a 1:1 mapping between ingestion trigger: ingestion result processing job
     */
    INGESTION_RESULT_PROCESSING_JOB,
    /**
     * Generic per integration job. This does not process ingestion results,
     * but rather runs arbitrary code. The scheduler will not set the payload
     * for these jobs.
     */
    GENERIC_INTEGRATION_JOB,
    /**
     * Generic per tenant job. This does not process ingestion results,
     * but rather runs arbitrary code. The scheduler will not set the payload
     * for these jobs.
     */
    GENERIC_TENANT_JOB;

    public boolean isGeneric() {
        return this == GENERIC_INTEGRATION_JOB || this == GENERIC_TENANT_JOB;
    }

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
