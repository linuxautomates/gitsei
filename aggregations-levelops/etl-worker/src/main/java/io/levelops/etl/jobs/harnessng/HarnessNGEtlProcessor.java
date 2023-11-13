package io.levelops.etl.jobs.harnessng;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class HarnessNGEtlProcessor extends BaseIngestionResultEtlProcessor<HarnessNGJobState> {
    private final HarnessNGPipelineStage harnessNGPipelineStage;

    @Autowired
    protected HarnessNGEtlProcessor(HarnessNGPipelineStage harnessNGPipelineStage) {
        super(HarnessNGJobState.class);
        this.harnessNGPipelineStage = harnessNGPipelineStage;
    }

    @Override
    public void preProcess(JobContext context, HarnessNGJobState jobState) {
    }

    @Override
    public void postProcess(JobContext context, HarnessNGJobState jobState) {
    }

    @Override
    public HarnessNGJobState createState(JobContext context) {
        return new HarnessNGJobState();
    }

    @Override
    public List<IngestionResultProcessingStage<?, HarnessNGJobState>> getIngestionProcessingJobStages() {
        return List.of(harnessNGPipelineStage);
    }
}
