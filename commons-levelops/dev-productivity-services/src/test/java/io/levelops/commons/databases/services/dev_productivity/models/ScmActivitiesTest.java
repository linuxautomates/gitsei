package io.levelops.commons.databases.services.dev_productivity.models;

import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ScmActivitiesTest {
    private ScmActivityDetails buildScmActivityDetails(DayOfWeek dayOfWeek, AtomicInteger i, int delta) {
        ScmActivityDetails scmActivityDetails = ScmActivityDetails.builder()
                .dayOfWeek(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                .numPrsCreated(i.getAndAdd(delta))
                .numPrsMerged(i.getAndAdd(delta))
                .numPrsClosed(i.getAndAdd(delta))
                .numPrsComments(i.getAndAdd(delta))
                .numCommitsCreated(i.getAndAdd(delta))
                .build();
        return scmActivityDetails;
    }

    private List<ScmActivityDetails> buildScmActivityDetailsList(AtomicInteger i, int delta) {
        List<ScmActivityDetails> scmActivityDetailsList = new ArrayList<>();
        for(DayOfWeek d : DayOfWeek.values()) {
            ScmActivityDetails scmActivityDetails = buildScmActivityDetails(d, i, delta);
            scmActivityDetailsList.add(scmActivityDetails);
        }
        return scmActivityDetailsList;
    }

    @Test
    public void testMergeScmActivities1() {
        AtomicInteger ai = new AtomicInteger(1);

        List<ScmActivities> scmActivitiesList = new ArrayList<>();
        for(int i=0; i< 2; i++) {
            List<ScmActivityDetails> scmActivityDetailsList = buildScmActivityDetailsList(ai, 1);
            ScmActivities scmActivities = ScmActivities.builder()
                    .integrationUserId(UUID.randomUUID().toString()).userName("user-name")
                    .activityDetails(scmActivityDetailsList)
                    .build();

            scmActivitiesList.add(scmActivities);
        }

        ScmActivities mergedSCMActivity = ScmActivities.mergeScmActivities(scmActivitiesList);

        AtomicInteger resultAI = new AtomicInteger(37);
        List<ScmActivityDetails> scmActivityDetailsList = buildScmActivityDetailsList(resultAI, 2);

        ScmActivities expected = ScmActivities.builder()
                .integrationUserId(scmActivitiesList.get(0).getIntegrationUserId()).userName("user-name")
                .activityDetails(scmActivityDetailsList)
                .build();

        Assert.assertEquals(expected, mergedSCMActivity);
    }

    @Test
    public void testMergeScmActivities2() {
        AtomicInteger ai = new AtomicInteger(1);

        List<ScmActivities> scmActivitiesList = new ArrayList<>();
        for(int i=0; i< 3; i++) {
            List<ScmActivityDetails> scmActivityDetailsList = buildScmActivityDetailsList(ai, 1);
            ScmActivities scmActivities = ScmActivities.builder()
                    .integrationUserId(UUID.randomUUID().toString()).userName("user-name")
                    .activityDetails(scmActivityDetailsList)
                    .build();

            scmActivitiesList.add(scmActivities);
        }

        ScmActivities mergedSCMActivity = ScmActivities.mergeScmActivities(scmActivitiesList);

        AtomicInteger resultAI = new AtomicInteger(108);
        List<ScmActivityDetails> scmActivityDetailsList = buildScmActivityDetailsList(resultAI, 3);

        ScmActivities expected = ScmActivities.builder()
                .integrationUserId(scmActivitiesList.get(0).getIntegrationUserId()).userName("user-name")
                .activityDetails(scmActivityDetailsList)
                .build();

        Assert.assertEquals(expected, mergedSCMActivity);
    }
}