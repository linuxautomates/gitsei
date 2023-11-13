package io.levelops.commons.databases.services.business_alignment;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.response.BaAllocation;
import io.levelops.commons.utils.ListUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaActiveWorkScoreUtils {

    @Value
    @Builder(toBuilder = true)
    public static class Scores {
        Integer alignmentScore;
        Float percentageScore;
        Map<String, BaAllocation> categoryAllocation;
    }

    /**
     * @return left is total score and right is categoryAllocations
     */
    public static Scores processActiveWorkEffortArray(List<Map<String, Object>> effortArray, TicketCategorizationScheme scheme) {
        Map<String, Integer> categoryToEffortMap = new HashMap<>();
        MutableInt totalMutable = new MutableInt(0);
        ListUtils.emptyIfNull(effortArray).forEach(item -> {
            String ticketCategory = (String) item.get("ticket_category");
            Object effortObj = item.get("effort");
            Integer effort = effortObj != null ? (effortObj instanceof Double ? ((Double)effortObj).intValue() : (Integer) effortObj) : 0;
            if (StringUtils.isBlank(ticketCategory)) {
                totalMutable.setValue(effort);
            } else {
                categoryToEffortMap.put(ticketCategory, effort);
            }
        });

        // add missing categories
        scheme.retrieveCategories().forEach(cat -> categoryToEffortMap.putIfAbsent(cat, 0));

        int total = totalMutable.getValue();
        MutableFloat totalWeightedScore = new MutableFloat(0);
        MutableFloat totalWeights = new MutableFloat(0);
        MutableFloat totalPercentageScore = new MutableFloat(0);
        MutableInt categoriesWithGoals = new MutableInt(0);
        Map<String, BaAllocation> categoryAllocation = new HashMap<>();
        categoryToEffortMap.forEach((category, effort) -> {
            float allocation = (total > 0) ? (effort.floatValue() / total) : 0;
            CategoryScores score = calculateAlignmentScore(scheme, category, allocation);
            int categoryScore = score.getAlignmentScore();
            float categoryWeight = score.getWeight();
            if (score.isGoals()) {
                categoriesWithGoals.increment();
            }
            totalWeightedScore.add(categoryScore * categoryWeight);
            totalWeights.add(categoryWeight);
            totalPercentageScore.add(score.getPercentageScore());
            categoryAllocation.put(category, BaAllocation.builder()
                    .alignmentScore(categoryScore)
                    .allocation(allocation)
                    .percentageScore(score.getPercentageScore())
                    .effort(effort)
                    .totalEffort(total)
                    .build());
        });

        // -- total alignment score
        float totalScore = totalWeights.floatValue() > 0 ? totalWeightedScore.floatValue() / totalWeights.floatValue() : 0;
        int normalizedScore = 0;
        if (totalScore >= 1f && totalScore <= 5f / 3f) {
            normalizedScore = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.POOR.getScore();
        } else if (totalScore >= 5f / 3f && totalScore <= 7f / 3f) {
            normalizedScore = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.FAIR.getScore();
        } else if (totalScore >= 7f / 3f && totalScore <= 3f) {
            normalizedScore = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.GOOD.getScore();
        }

        return Scores.builder()
                .alignmentScore(normalizedScore)
                .categoryAllocation(categoryAllocation)
                .percentageScore(categoriesWithGoals.getValue() > 0 ? totalPercentageScore.getValue() / categoriesWithGoals.getValue() : 0)
                .build();
    }

    @Value
    @Builder
    public static class CategoryScores {
        Integer alignmentScore;
        Float weight;
        Float percentageScore;
        boolean goals;
    }

    /**
     * See https://levelops.atlassian.net/wiki/spaces/PM/pages/1615560705/Business+Alignment+2.0+-+Reports#Computation
     *
     * @return left is alignment score and right is category weight to use for total alignment score
     */
    @Nonnull
    public static CategoryScores calculateAlignmentScore(TicketCategorizationScheme scheme, String category, float allocation) {
        TicketCategorizationScheme.Goals goals = scheme.retrieveGoals(category).orElse(null);
        if (goals == null) {
            return CategoryScores.builder()
                    .alignmentScore(0)
                    .weight(0f)
                    .percentageScore(0f)
                    .goals(false)
                    .build();
        }
        float idealMin = (float) MoreObjects.firstNonNull(goals.getIdealRange().getMin(), 0) / 100f;
        float idealMax = (float) MoreObjects.firstNonNull(goals.getIdealRange().getMax(), 0) / 100f;
        idealMax = Math.min(idealMax, 1f);
        idealMin = Math.max(0f, idealMin);
        idealMin = Math.min(idealMin, idealMax);
        float acceptMin = (float) MoreObjects.firstNonNull(goals.getAcceptableRange().getMin(), 0) / 100f;
        float acceptMax = (float) MoreObjects.firstNonNull(goals.getAcceptableRange().getMax(), 0) / 100f;
        acceptMin = Math.min(acceptMin, idealMin);
        acceptMax = Math.max(acceptMax, idealMax);

        float categoryWeight = (idealMin + idealMax) / 2f; // mid-point of ideal range

        // See https://levelops.atlassian.net/browse/LEV-4408 for percentageScore details
        float percentageScore;
        BaJiraAggsActiveWorkQueryBuilder.AlignmentScore score;
        if (allocation >= idealMin && allocation <= idealMax) {
            score = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.GOOD;
            percentageScore = 1f;
        } else if (allocation >= acceptMin && allocation <= acceptMax) {
            score = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.FAIR;
            if (acceptMax > acceptMin) {
                if (allocation <= idealMin) {
                    percentageScore = (allocation - acceptMin) / (idealMin - acceptMin) * .5f + .5f;
                } else {
                    percentageScore = (acceptMax - allocation) / (acceptMax - idealMax) * .5f + .5f;
                }
            } else {
                percentageScore = 0.5f;
            }

        } else {
            score = BaJiraAggsActiveWorkQueryBuilder.AlignmentScore.POOR;
            percentageScore = 0f;
        }
        return CategoryScores.builder()
                .alignmentScore(score.getScore())
                .weight(categoryWeight)
                .percentageScore(percentageScore)
                .goals(true)
                .build();
    }

}
