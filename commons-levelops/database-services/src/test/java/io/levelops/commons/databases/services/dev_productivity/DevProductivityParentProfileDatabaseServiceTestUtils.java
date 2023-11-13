package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DevProductivityParentProfileDatabaseServiceTestUtils {
    public static DevProductivityParentProfile createDevProdParentProfile(DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService, String company, String ticketCategorizationSchemeId, int i, List<Integer> ouRefIds) throws SQLException {
        DevProductivityProfile.Section section1 = DevProductivityProfile.Section.builder()
                .name("Quality of Work → Type of Work").description("Quality of Work → Type of Work").order(0).enabled(true)
                .features(List.of(
                        DevProductivityProfile.Feature.builder()
                                .name("Percentage of Legacy Rework").description("Percentage of Legacy Rework")
                                .order(0)
                                .enabled(true)
                                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                                .maxValue(70L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                .build(),
                        DevProductivityProfile.Feature.builder()
                                .name("Bugs per 100 LoC").description("Bugs per 100 LoC")
                                .order(1)
                                .enabled(true)
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
                                .enabled(true)
                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_COMMITS_PER_MONTH)
                                .maxValue(35L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                //   .ticketCategories(List.of(UUID.randomUUID()))
                                .build(),
                        DevProductivityProfile.Feature.builder()
                                .name("Number of PRs per month").description("Number of PRs per month")
                                .order(1)
                                .enabled(true)
                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                .maxValue(150L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                //      .ticketCategories(List.of(UUID.randomUUID(), UUID.randomUUID()))
                                .build()

                ))
                .build();
        DevProductivityProfile.DevProductivityProfileBuilder bldr = DevProductivityProfile.builder()
                .id(UUID.randomUUID())
                .order(0)
                .enabled(true)
                .name("Default Profile " + i).description("Default Profile " + i)
                .defaultProfile(true)
                .sections(List.of(section1, section2))
                //.effortInvestmentProfileId(UUID.fromString(ticketCategorizationSchemeId))
                .settings(Map.of());
        DevProductivityProfile profile = bldr.build();

        DevProductivityParentProfile devProductivityParentProfile = DevProductivityParentProfile.builder()
                .name("Parent profile")
                .effortInvestmentProfileId(UUID.fromString(ticketCategorizationSchemeId))
                .associatedOURefIds(ouRefIds.stream().map(ouRefId -> ouRefId.toString()).collect(Collectors.toList()))
                .subProfiles(List.of(profile))
                .build();


        String devProdParentProfileId = devProductivityParentProfileDatabaseService.insert(company, devProductivityParentProfile);
        return devProductivityParentProfile.toBuilder().id(UUID.fromString(devProdParentProfileId)).build();

    }

    public static Boolean updateProfile(DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService, String company, DevProductivityParentProfile p) throws SQLException {
        return devProductivityParentProfileDatabaseService.update(company, p);
    }

    public static DevProductivityParentProfile createDevProdParentProfile(DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService, String company, String ticketCategorizationSchemeId, int i) throws SQLException {
        return createDevProdParentProfile(devProductivityParentProfileDatabaseService, company, ticketCategorizationSchemeId, i, null);
    }

    public static List<DevProductivityParentProfile> createDevProdParentProfiles(DevProductivityParentProfileDatabaseService devProductivityProfileDatabaseService, String company, String ticketCategorizationSchemeId, int n) throws SQLException {
        List<DevProductivityParentProfile> result = new ArrayList<>();
        for (int i =0; i<n; i++) {
            DevProductivityParentProfile profile = createDevProdParentProfile(devProductivityProfileDatabaseService, company, ticketCategorizationSchemeId, i, List.of(i));
            result.add(profile);
        }
        return result;
    }

    public static DevProductivityParentProfile loadCentralProfileTemplate() throws IOException {
        String resourceString = ResourceUtils.getResourceAsString("json/databases/dev_prod/central_profile.json");
        DevProductivityParentProfile centralProfile = DefaultObjectMapper.get().readValue(resourceString,DevProductivityParentProfile.class);
        return centralProfile;
    }
}
