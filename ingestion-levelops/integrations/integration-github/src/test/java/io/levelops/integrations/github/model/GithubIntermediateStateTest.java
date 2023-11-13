package io.levelops.integrations.github.model;

import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.github.model.GithubIntermediateState.Stage;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubIntermediateStateTest {


    @Test
    public void parseIntermediateState() {
        GithubIntermediateState state = GithubIntermediateState.parseIntermediateState(Map.of(
                "completed_stages", List.of("commits", "prs"),
                "resume_from_repo", "test"));
        assertThat(state).isEqualTo(GithubIntermediateState.builder()
                .completedStages(List.of(Stage.COMMITS, Stage.PRS))
                .resumeFromRepo("test")
                .build());
    }

    @Test
    public void markStageAsCompleted() {
        GithubIntermediateState state = GithubIntermediateState.markStageAsCompleted(
                GithubIntermediateState.builder()
                        .completedStages(List.of(Stage.COMMITS, Stage.PRS))
                        .resumeFromRepo("test")
                        .build(),
                Stage.ISSUES
        );
        assertThat(state).isEqualTo(  GithubIntermediateState.builder()
                .completedStages(List.of(Stage.COMMITS, Stage.PRS, Stage.ISSUES))
                .build());
    }

    @Test
    public void updateJobContext() {
        JobContext context = GithubIntermediateState.updateJobContext(JobContext.builder()
                        .jobId("123")
                        .intermediateState(Map.of("something", "else"))
                        .build(),
                GithubIntermediateState.builder()
                        .completedStages(List.of(Stage.COMMITS, Stage.PRS))
                        .resumeFromRepo("test")
                        .build()
        );
        assertThat(context).isEqualTo(  JobContext.builder()
                        .jobId("123")
                        .intermediateState(Map.of(
                                "completed_stages", List.of("commits", "prs"),
                                "resume_from_repo", "test"))
                .build());
    }
}