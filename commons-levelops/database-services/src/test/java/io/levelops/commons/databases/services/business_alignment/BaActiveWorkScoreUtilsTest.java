package io.levelops.commons.databases.services.business_alignment;


import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.response.BaAllocation;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;

public class BaActiveWorkScoreUtilsTest {

    static TicketCategorizationScheme scheme = TicketCategorizationScheme.builder()
            .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                    .uncategorized(TicketCategorizationScheme.Uncategorized.builder()
                            .goals(TicketCategorizationScheme.Goals.builder()
                                    .enabled(true)
                                    .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                    .acceptableRange(new TicketCategorizationScheme.Goal(20, 30))
                                    .build())
                            .build())
                    .categories(Map.of(
                            "cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                    .name("cat1")
                                    .goals(TicketCategorizationScheme.Goals.builder()
                                            .enabled(true)
                                            .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                            .acceptableRange(new TicketCategorizationScheme.Goal(20, 45))
                                            .build())
                                    .build(),
                            "cat2", TicketCategorizationScheme.TicketCategorization.builder()
                                    .name("cat2")
                                    .goals(TicketCategorizationScheme.Goals.builder()
                                            .enabled(true)
                                            .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                            .acceptableRange(new TicketCategorizationScheme.Goal(20, 30))
                                            .build())
                                    .build(),
                            "cat3", TicketCategorizationScheme.TicketCategorization.builder()
                                    .name("cat3")
                                    .goals(TicketCategorizationScheme.Goals.builder()
                                            .enabled(true)
                                            .idealRange(new TicketCategorizationScheme.Goal(30, 40))
                                            .acceptableRange(new TicketCategorizationScheme.Goal(20, 50))
                                            .build())
                                    .build()))
                    .activeWork(TicketCategorizationScheme.ActiveWork.builder()
                            .issues(TicketCategorizationScheme.IssuesActiveWork.builder()
                                    .assigned(true)
                                    .activeSprints(true)
                                    .inProgress(true)
                                    .build())
                            .build())
                    .build())
            .build();

    @Test
    public void calculateAlignmentScore() {
        checkAlignmentScore("wrong", 0f, 0, 0f, 0f, false);

        checkAlignmentScore("cat1", 0.1f, 1, .25f, 0f, true);
        checkAlignmentScore("cat1", 0.25f, 3, .25f, 1f, true);
        checkAlignmentScore("cat1", .35f, 2, .25f, 0.8333334f, true);
        checkAlignmentScore("cat1", .40f, 2, .25f, 0.6666666f, true);
        checkAlignmentScore("cat1", .45f, 2, .25f, 0.5f, true);
        checkAlignmentScore("cat1", .50f, 1, .25f, 0f, true);

        checkAlignmentScore("cat2", 0.1f, 1, .25f, 0f, true);
        checkAlignmentScore("cat2", 0.25f, 3, .25f, 1f, true);
        checkAlignmentScore("cat2", .35f, 1, .25f, 0f, true);
        checkAlignmentScore("cat2", .40f, 1, .25f, 0f, true);
        checkAlignmentScore("cat2", .45f, 1, .25f, 0f, true);
        checkAlignmentScore("cat2", .50f, 1, .25f, 0f, true);

        checkAlignmentScore("cat3", 0.1f, 1, .35f, 0f, true);
        checkAlignmentScore("cat3", 0.2f, 2, .35f, .5f, true);
        checkAlignmentScore("cat3", 0.23f, 2, .35f, .65f, true);
        checkAlignmentScore("cat3", 0.25f, 2, .35f, .75f, true);
        checkAlignmentScore("cat3", 0.27f, 2, .35f, .85f, true);
        checkAlignmentScore("cat3", 0.3f, 3, .35f, 1f, true);
        checkAlignmentScore("cat3", 0.4f, 3, .35f, 1f, true);
        checkAlignmentScore("cat3", 0.45f, 2, .35f, .75f, true);
        checkAlignmentScore("cat3", 0.5f, 2, .35f, .5f, true);
        checkAlignmentScore("cat3", 0.6f, 1, .35f, 0f, true);
    }

    @Test
    public void processActiveWorkEffortArray() {
        BaActiveWorkScoreUtils.Scores scores = BaActiveWorkScoreUtils.processActiveWorkEffortArray(
                List.of(Map.of( "effort", 100), // total
                        Map.of("ticket_category", "cat1", "effort", 35),
                        Map.of("ticket_category", "cat2", "effort", 25),
                        Map.of("ticket_category", "cat3", "effort", 60),
                        Map.of("ticket_category", "no-goals", "effort", 10)),
                scheme);
        DefaultObjectMapper.prettyPrint(scores);
        assertThat(scores.getCategoryAllocation().get("cat1")).isEqualTo(BaAllocation.builder()
                .alignmentScore(2)
                .percentageScore(0.8333334f)
                .allocation(.35f)
                .effort(35)
                .totalEffort(100)
                .build());
        assertThat(scores.getCategoryAllocation().get("cat2")).isEqualTo(BaAllocation.builder()
                .alignmentScore(3)
                .percentageScore(1f)
                .allocation(.25f)
                .effort(25)
                .totalEffort(100)
                .build());
        assertThat(scores.getCategoryAllocation().get("cat3")).isEqualTo(BaAllocation.builder()
                .alignmentScore(1)
                .percentageScore(0f)
                .allocation(.6f)
                .effort(60)
                .totalEffort(100)
                .build());
        assertThat(scores.getCategoryAllocation().get("no-goals")).isEqualTo(BaAllocation.builder()
                .alignmentScore(0)
                .percentageScore(0f)
                .allocation(0.1f)
                .effort(10)
                .totalEffort(100)
                .build());
        assertThat(scores.getCategoryAllocation().get("Other")).isEqualTo(BaAllocation.builder()
                .alignmentScore(1)
                .percentageScore(0f)
                .allocation(0f)
                .effort(0)
                .totalEffort(100)
                .build());
        assertThat(scores.getAlignmentScore()).isEqualTo(2);
        assertThat(scores.getPercentageScore()).isEqualTo(.45f, withPrecision(0.01f));
    }

    private void checkAlignmentScore(String cat, float alloc, int alignmentScore, float weight, float percentageScore, boolean goals) {
        BaActiveWorkScoreUtils.CategoryScores scores = BaActiveWorkScoreUtils.calculateAlignmentScore(scheme, cat, alloc);
        assertThat(scores.getAlignmentScore())
                .overridingErrorMessage("Expected AlignmentScore for category '%s' to be equal to '%s' but got '%s'", cat, alignmentScore, scores.getAlignmentScore())
                .isEqualTo(alignmentScore);
        assertThat(scores.getWeight())
                .overridingErrorMessage("Expected weight for category '%s' to be equal to '%s' but got '%s'", cat, weight, scores.getWeight())
                .isEqualTo(weight, withPrecision(0.01f));
        assertThat(scores.getPercentageScore())
                .overridingErrorMessage("Expected percentage score for category '%s' to be equal to '%s' but got '%s'", cat, percentageScore, scores.getPercentageScore())
                .isEqualTo(percentageScore, withPrecision(0.01f));
        assertThat(scores.isGoals())
                .overridingErrorMessage("Expected goals for category '%s' to be equal to '%s' but got '%s'", cat, goals, scores.isGoals())
                .isEqualTo(goals);
    }

}