package io.levelops.ingestion.engine.controllers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestController implements DataController<TestController.TestQuery> {

    private final int sleepMs;
    private final boolean generateOOM;
    private final boolean throwOOM;

    public TestController(int sleepMs) {
        this(sleepMs, false, false);
    }

    public TestController(int sleepMs, boolean generateOOM, boolean throwOOM) {
        this.sleepMs = sleepMs;
        this.generateOOM = generateOOM;
        this.throwOOM = throwOOM;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, TestQuery query) throws IngestException {
        log.info("Started");
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IngestException(e);
        }
        if (throwOOM) {
            throw new OutOfMemoryError();
        }
        if (generateOOM) {
            generateOOM();
        }
        log.info("Stopped");
        return TestResult.builder().msg(jobContext.getJobId()).build();
    }

    private void generateOOM() {
        int dummyArraySize = 15;
        System.out.println("Free JVM memory: " + Runtime.getRuntime().freeMemory());
        System.out.println("Total JVM memory: " + Runtime.getRuntime().totalMemory());
        System.out.println("Max JVM memory: " + Runtime.getRuntime().maxMemory());
        long memoryConsumed = 0;
        try {
            long[] memoryAllocated = null;
            for (int loop = 0; loop < Integer.MAX_VALUE; loop++) {
                memoryAllocated = new long[dummyArraySize];
                memoryAllocated[0] = 0;
                memoryConsumed += dummyArraySize * Long.SIZE;
                System.out.println("Memory Consumed till now: " + memoryConsumed);
                dummyArraySize *= dummyArraySize * 2;
            }
        } catch (OutOfMemoryError outofMemory) {
            System.out.println("Catching out of memory error");
            //Log the information,so that we can generate the statistics (latter on).
            throw outofMemory;
        }
    }


    @Override
    public TestQuery parseQuery(Object o) {
        return new TestQuery();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TestResult.TestResultBuilder.class)
    public static class TestResult implements ControllerIngestionResult {
        String msg;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TestQuery.TestQueryBuilder.class)
    public static class TestQuery implements DataQuery {

    }
}
