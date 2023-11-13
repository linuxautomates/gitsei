package io.levelops.etl.jobs.jira_user_emails;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseGenericEtlProcessor;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JiraUserEmailsEtlProcessor extends BaseGenericEtlProcessor<JiraUserEmailsJobState> {
    private final UserEmailsStage userEmailsStage;
    protected JiraUserEmailsEtlProcessor(UserEmailsStage userEmailsStage) {
        super(JiraUserEmailsJobState.class);
        this.userEmailsStage = userEmailsStage;
    }

    @Override
    public List<GenericJobProcessingStage<JiraUserEmailsJobState>> getGenericJobProcessingStages() {
        return List.of(userEmailsStage);
    }

    @Override
    public void preProcess(JobContext context, JiraUserEmailsJobState jobState) {

    }

    @Override
    public void postProcess(JobContext context, JiraUserEmailsJobState jobState) {

    }

    @Override
    public JiraUserEmailsJobState createState(JobContext context) {
        return new JiraUserEmailsJobState();
    }
}
