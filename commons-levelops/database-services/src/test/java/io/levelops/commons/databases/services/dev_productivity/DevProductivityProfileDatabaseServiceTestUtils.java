package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DevProductivityProfileDatabaseServiceTestUtils {
    public static DevProductivityProfile createDevProdProfile(DevProductivityProfileDatabaseService devProductivityProfileDatabaseService, String company, String ticketCategorizationSchemeId, int i, List<Integer> ouRefIds) throws SQLException {
        DevProductivityProfile.Section section1 = DevProductivityProfile.Section.builder()
                .name("Quality of Work → Type of Work").description("Quality of Work → Type of Work").order(0).enabled(true)
                .features(List.of(
                        DevProductivityProfile.Feature.builder()
                                .name("Percentage of Legacy Rework").description("Percentage of Legacy Rework")
                                .order(0)
                                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                                .maxValue(70L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                .build(),
                        DevProductivityProfile.Feature.builder()
                                .name("Bugs per 100 LoC").description("Bugs per 100 LoC")
                                .order(1)
                                .featureType(DevProductivityProfile.FeatureType.BUGS_PER_HUNDRED_LINES_OF_CODE)
                                .maxValue(100L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                .build()
                ))
                .build();

        DevProductivityProfile.Section section2 = DevProductivityProfile.Section.builder()
                .name("Quality of Work → Type of Work").description("Quality of Work → Type of Work").order(1).weight(9).enabled(true)
                .features(List.of(
                        DevProductivityProfile.Feature.builder()
                                .name("Number of Commits per month").description("Number of Commits per month")
                                .order(0)
                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_COMMITS_PER_MONTH)
                                .maxValue(35L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                //   .ticketCategories(List.of(UUID.randomUUID()))
                                .build(),
                        DevProductivityProfile.Feature.builder()
                                .name("Number of PRs per month").description("Number of PRs per month")
                                .order(1)
                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                .maxValue(150L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                //      .ticketCategories(List.of(UUID.randomUUID(), UUID.randomUUID()))
                                .build()

                ))
                .build();
        DevProductivityProfile.DevProductivityProfileBuilder bldr = DevProductivityProfile.builder()
                .id(UUID.randomUUID())
                .name("Default Profile " + i).description("Default Profile " + i)
                .defaultProfile(true)
                .sections(List.of(section1, section2))
                .effortInvestmentProfileId(UUID.fromString(ticketCategorizationSchemeId))
                .settings(Map.of());
        if(CollectionUtils.isNotEmpty(ouRefIds)) {
            bldr.associatedOURefIds(ouRefIds.stream().map(ouRefId -> ouRefId.toString()).collect(Collectors.toList()));
        }

        DevProductivityProfile devProductivityProfile = bldr.build();
        String devProdProfileId = devProductivityProfileDatabaseService.insert(company, devProductivityProfile);
        return devProductivityProfile.toBuilder().id(UUID.fromString(devProdProfileId)).build();

    }

    public static Boolean updateProfile(DevProductivityProfileDatabaseService devProductivityProfileDatabaseService, String company, DevProductivityProfile p) throws SQLException {
        return devProductivityProfileDatabaseService.update(company, p);
    }

    public static DevProductivityProfile createDevProdProfile(DevProductivityProfileDatabaseService devProductivityProfileDatabaseService, String company, String ticketCategorizationSchemeId, int i) throws SQLException {
        return createDevProdProfile(devProductivityProfileDatabaseService, company, ticketCategorizationSchemeId, i, null);
    }

    public static List<DevProductivityProfile> createDevProdProfiles(DevProductivityProfileDatabaseService devProductivityProfileDatabaseService, String company, String ticketCategorizationSchemeId, int n) throws SQLException {
        List<DevProductivityProfile> result = new ArrayList<>();
        for (int i =0; i<n; i++) {
            DevProductivityProfile profile = createDevProdProfile(devProductivityProfileDatabaseService, company, ticketCategorizationSchemeId, i);
            result.add(profile);
        }
        return result;
    }
}
