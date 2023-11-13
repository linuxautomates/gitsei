package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.integrations.jira.models.JiraIssue;
import org.junit.Test;

import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BaseIngestionResultProcessingStageTest {
    public static class MyState {
        public String name;
    }
    public static class SampleProcessingStage extends BaseIngestionResultProcessingStage<JiraIssue, MyState> {

        @Override
        public String getName() {
            return "testName";
        }

        @Override
        public void preStage(JobContext context, MyState jobState) throws SQLException {

        }

        @Override
        public void process(JobContext context, MyState jobState, String ingestionJobId, JiraIssue entity) throws SQLException {

        }

        @Override
        public void postStage(JobContext context, MyState jobState) {

        }

        @Override
        public String getDataTypeName() {
            return "jira_issues";
        }

        @Override
        public boolean onlyProcessLatestIngestionJob() {
            return false;
        }
    }

    @Test
    public void testStageReflection() {
        SampleProcessingStage stage = new SampleProcessingStage();
        assertThat(stage.getGcsDataType()).isEqualTo(JiraIssue.class);
    }
}
