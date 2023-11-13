package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.utils.DevProductivityProfileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DevProductivityProfileUtilsTest {

    @Test
    public void testDevProductivityProfileComparison(){
        DevProductivityParentProfile p1 = DevProductivityParentProfile.builder()
                .name("Profile 1")
                .associatedOURefIds(List.of("1","2"))
                .effortInvestmentProfileId(UUID.randomUUID())
                .settings(Map.of("development_stages","In-progress"))
                .subProfiles(
                        List.of(DevProductivityProfile.builder()
                                .name("sub-profile 1")
                                .enabled(true)
                                .order(1)
                                .matchingCriteria(List.of(DevProductivityProfile.MatchingCriteria.builder()
                                        .field("role").condition(DevProductivityProfile.MatchingCondition.EQ).values(List.of("Dev"))
                                        .build()))
                                .sections(
                                        List.of(DevProductivityProfile.Section.builder()
                                                .name("Section 1")
                                                .enabled(true)
                                                .order(1)
                                                .weight(10)
                                                .features(List.of(DevProductivityProfile.Feature.builder()
                                                        .name("feature 1")
                                                        .enabled(true)
                                                        .featureType(DevProductivityProfile.FeatureType.AVG_CODING_DAYS_PER_WEEK)
                                                        .maxValue(4l)
                                                        .lowerLimitPercentage(25)
                                                        .upperLimitPercentage(75)
                                                        .ticketCategories(List.of(UUID.randomUUID()))
                                                        .build()
                                                ))
                                                .build())
                                )
                                .build())
                ).build();
        //no change
        DevProductivityParentProfile p2 = p1.toBuilder().build();
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

        //profile change
        p2 = p1.toBuilder().name("Profile 1 - edit").build();
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

        //sub-profile change
        DevProductivityProfile subProfile1 = p1.getSubProfiles().get(0);

        DevProductivityProfile subProfile2 = subProfile1.toBuilder().name("Edited sub-profile").build();
        p2 = p1.toBuilder().subProfiles(List.of(subProfile2)).build();
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSame(subProfile1, subProfile2));
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSameRecursive(subProfile1, subProfile2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

        subProfile2 = subProfile1.toBuilder().enabled(false).build();
        p2 = p1.toBuilder().subProfiles(List.of(subProfile2)).build();
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSame(subProfile1, subProfile2));
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSameRecursive(subProfile1, subProfile2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

        subProfile2 = subProfile1.toBuilder().matchingCriteria(null).build();
        p2 = p1.toBuilder().subProfiles(List.of(subProfile2)).build();
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSame(subProfile1, subProfile2));
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSameRecursive(subProfile1, subProfile2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));


        //section change
        DevProductivityProfile.Section s1 = subProfile1.getSections().get(0);
        DevProductivityProfile.Section s2 = s1.toBuilder().enabled(false).build();
        subProfile2 = subProfile1.toBuilder().sections(List.of(s2)).build();
        p2 = p1.toBuilder().subProfiles(List.of(subProfile2)).build();
        Assert.assertTrue(DevProductivityProfileUtils.isSubProfileSame(subProfile1, subProfile2));
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSameRecursive(subProfile1, subProfile2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

        //feature change
        DevProductivityProfile.Feature f1 = s1.getFeatures().get(0);
        DevProductivityProfile.Feature f2 = f1.toBuilder().ticketCategories(List.of(UUID.randomUUID())).build();
        s2 = s1.toBuilder().features(List.of(f2)).build();
        subProfile2 = subProfile1.toBuilder().sections(List.of(s2)).build();
        p2 = p1.toBuilder().subProfiles(List.of(subProfile2)).build();
        Assert.assertTrue(DevProductivityProfileUtils.isSubProfileSame(subProfile1, subProfile2));
        Assert.assertFalse(DevProductivityProfileUtils.isSubProfileSameRecursive(subProfile1, subProfile2));
        Assert.assertTrue(DevProductivityProfileUtils.isParentProfileSame(p1, p2));
        Assert.assertFalse(DevProductivityProfileUtils.isParentProfileSameRecursive(p1, p2));

    }
}
