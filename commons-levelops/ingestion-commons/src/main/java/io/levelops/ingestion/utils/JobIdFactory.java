package io.levelops.ingestion.utils;

import org.apache.logging.log4j.util.Strings;

import java.util.UUID;

public class JobIdFactory {

    public static String generateNewJobId() {
        return UUID.randomUUID().toString();
    }

    public static String useJobIdOrGenerateNew(String jobId) {
        if (Strings.isNotEmpty(jobId)) {
            return jobId;
        }
        return generateNewJobId();
    }

}
