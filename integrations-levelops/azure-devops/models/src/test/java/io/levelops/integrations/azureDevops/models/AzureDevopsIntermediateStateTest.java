package io.levelops.integrations.azureDevops.models;

import io.levelops.ingestion.models.JobContext;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AzureDevopsIntermediateStateTest {

    @Test
    public void parseIntermediateState() {
        AzureDevopsIntermediateState state = AzureDevopsIntermediateState.parseIntermediateState(Map.of(
                "completed_stages", List.of("commits", "prs"),
                "resume_from_project", "test"));
        assertThat(state).isEqualTo(AzureDevopsIntermediateState.builder()
                .completedStages(List.of(AzureDevopsIntermediateState.Stage.COMMITS,
                        AzureDevopsIntermediateState.Stage.PRS))
                .resumeFromProject("test")
                .build());
    }

    @Test
    public void markStageAsCompleted() {
        AzureDevopsIntermediateState state = AzureDevopsIntermediateState.markStageAsCompleted(
                AzureDevopsIntermediateState.builder()
                        .completedStages(List.of(AzureDevopsIntermediateState.Stage.COMMITS,
                                AzureDevopsIntermediateState.Stage.PRS))
                        .resumeFromProject("test")
                        .build(),
                AzureDevopsIntermediateState.Stage.WORKITEM_HISTORIES
        );
        assertThat(state).isEqualTo(AzureDevopsIntermediateState.builder()
                .completedStages(List.of(AzureDevopsIntermediateState.Stage.COMMITS,
                        AzureDevopsIntermediateState.Stage.PRS,
                        AzureDevopsIntermediateState.Stage.WORKITEM_HISTORIES))
                .build());
    }

    @Test
    public void updateJobContext() {
        JobContext context = AzureDevopsIntermediateState.updateJobContext(JobContext.builder()
                        .jobId("123")
                        .intermediateState(Map.of("something", "else"))
                        .build(),
                AzureDevopsIntermediateState.builder()
                        .completedStages(List.of(AzureDevopsIntermediateState.Stage.COMMITS,
                                AzureDevopsIntermediateState.Stage.PRS))
                        .resumeFromProject("test")
                        .build()
        );
        assertThat(context).isEqualTo(  JobContext.builder()
                .jobId("123")
                .intermediateState(Map.of(
                        "completed_stages", List.of("commits", "prs"),
                        "resume_from_project", "test"))
                .build());
    }
}
