package io.levelops.etl.jobs.user_id_consolidation;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseGenericEtlProcessor;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserIdConsolidationEtlProcessor extends BaseGenericEtlProcessor<UserIdConsolidationState> {
    private final UserIdConsolidationStage userIdConsolidationStage;

    protected UserIdConsolidationEtlProcessor(UserIdConsolidationStage userIdConsolidationStage) {
        super(UserIdConsolidationState.class);
        this.userIdConsolidationStage = userIdConsolidationStage;
    }

    @Override
    public List<GenericJobProcessingStage<UserIdConsolidationState>> getGenericJobProcessingStages() {
        return List.of(userIdConsolidationStage);
    }

    @Override
    public void preProcess(JobContext context, UserIdConsolidationState jobState) {

    }

    @Override
    public void postProcess(JobContext context, UserIdConsolidationState jobState) {

    }

    @Override
    public UserIdConsolidationState createState(JobContext context) {
        return new UserIdConsolidationState();
    }
}
