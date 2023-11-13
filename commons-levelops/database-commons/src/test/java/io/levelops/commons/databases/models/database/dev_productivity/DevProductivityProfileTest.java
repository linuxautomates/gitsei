package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.Rating.ACCEPTABLE;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.Rating.GOOD;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.Rating.NEEDS_IMPROVEMENT;

public class DevProductivityProfileTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
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
                                .ticketCategories(List.of(UUID.randomUUID()))
                                .build(),
                        DevProductivityProfile.Feature.builder()
                                .name("Number of PRs per month").description("Number of PRs per month")
                                .order(1)
                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                .maxValue(150L)
                                .lowerLimitPercentage(25).upperLimitPercentage(75)
                                .ticketCategories(List.of(UUID.randomUUID(), UUID.randomUUID()))
                                .build()

                ))
                .build();

        DevProductivityProfile devProductivityProfile = DevProductivityProfile.builder()
                .id(UUID.randomUUID())
                .name("Default Profile").description("Default Profile")
                .defaultProfile(true)
                .sections(List.of(section1, section2))
                .effortInvestmentProfileId(UUID.randomUUID())
                .settings(Map.of())
                .build();

        String data = MAPPER.writeValueAsString(devProductivityProfile);
        DevProductivityProfile actual = MAPPER.readValue(data, DevProductivityProfile.class);
        Assert.assertEquals(devProductivityProfile, actual);
        Assert.assertNotNull(data);
    }

    @Test
    public void testMatchingCriteria() throws JsonProcessingException {
        String matchingCriteria = "[{\n" +
                "            \"field\": \"contributor_role\",\n" +
                "            \"values\": [\"Junior Software Engineer\"],\n" +
                "            \"condition\" : \"EQ\"\n" +
                "          }]";
        List<DevProductivityProfile.MatchingCriteria> mc  =MAPPER.readValue(matchingCriteria, MAPPER.getTypeFactory().constructCollectionType(List.class, DevProductivityProfile.MatchingCriteria.class));
        Assert.assertEquals(mc.get(0).getField(),"contributor_role");
        Assert.assertEquals(mc.get(0).getValues(), List.of("Junior Software Engineer"));
        Assert.assertEquals(mc.get(0).getCondition(), DevProductivityProfile.MatchingCondition.EQ);
    }

    @Test
    public void testSectionsList() throws JsonProcessingException {
        String sectionsString = "[{\"name\":\"Quality of Work → Type of Work\",\"description\":\"Quality of Work → Type of Work\",\"order\":0,\"weight\":9 ,\"features\":[{\"slow_to_good_is_ascending\":true,\"name\":\"New Features vs. Rework vs. Legacy work\",\"description\":\"New Features vs. Rework vs. Legacy work\",\"order\":0,\"type\":\"PERCENTAGE_OF_LEGACY_REWORK\",\"max_value\":70,\"lower_limit_percentage\":25,\"upper_limit_percentage\":75},{\"slow_to_good_is_ascending\":false,\"name\":\"Bugs per 100 LoC\",\"description\":\"Bugs per 100 LoC\",\"order\":1,\"type\":\"BUGS_PER_HUNDRED_LINES_OF_CODE\",\"max_value\":100,\"lower_limit_percentage\":25,\"upper_limit_percentage\":75}]},{\"name\":\"Quality of Work → Type of Work\",\"description\":\"Quality of Work → Type of Work\",\"order\":1,\"features\":[{\"slow_to_good_is_ascending\":false,\"name\":\"Percentage of Duplicated Code\",\"description\":\"Percentage of Duplicated Code\",\"order\":0,\"type\":\"SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE\",\"max_value\":35,\"lower_limit_percentage\":25,\"upper_limit_percentage\":75},{\"slow_to_good_is_ascending\":true,\"name\":\"Number of PRs per month\",\"description\":\"Number of PRs per month\",\"order\":1,\"type\":\"NUMBER_OF_PRS_PER_MONTH\",\"max_value\":150,\"lower_limit_percentage\":25,\"upper_limit_percentage\":75}]}]";
        List<DevProductivityProfile.Section> sections = MAPPER.readValue(sectionsString, MAPPER.getTypeFactory().constructCollectionType(List.class, DevProductivityProfile.Section.class));
        Assert.assertNotNull(sections);
    }

    @Test
    public void testRatingAndScore() {
        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .name("abc")
                .maxValue(100L).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH)
                .lowerLimitPercentage(20).upperLimitPercentage(80)
                .build();

        Assert.assertNull(feature.calculateScore(null));
        Assert.assertNull(feature.calculateRating(null));

        Assert.assertEquals(0, feature.calculateScore(0L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(0L));

        Assert.assertEquals(9, feature.calculateScore(10L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(10L));

        Assert.assertEquals(17, feature.calculateScore(19L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(19L));

        Assert.assertEquals(18, feature.calculateScore(20L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(20L));

        Assert.assertEquals(18, feature.calculateScore(21L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(21L));

        Assert.assertEquals(45, feature.calculateScore(50L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(50L));

        Assert.assertEquals(71, feature.calculateScore(79L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(79L));

        Assert.assertEquals(72, feature.calculateScore(80L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(80L));

        Assert.assertEquals(72, feature.calculateScore(81L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(81L));

        Assert.assertEquals(90, feature.calculateScore(100L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(100L));

        Assert.assertEquals(97, feature.calculateScore(700L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(101L));

        Assert.assertEquals(91, feature.calculateScore(110L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(110L));

        feature = DevProductivityProfile.Feature.builder()
                .name("abc")
                .maxValue(100L).maxUnit(TimeUnit.DAYS).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH)
                .lowerLimitPercentage(20).upperLimitPercentage(80)
                .build();

        Assert.assertNull(feature.calculateScore(null));
        Assert.assertNull(feature.calculateRating(null));

        Assert.assertEquals(0, feature.calculateScore(TimeUnit.DAYS.toSeconds(0L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(0L)));

        Assert.assertEquals(9, feature.calculateScore(TimeUnit.DAYS.toSeconds(10L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(10L)));

        Assert.assertEquals(17, feature.calculateScore(TimeUnit.DAYS.toSeconds(19L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(19L)));

        Assert.assertEquals(18, feature.calculateScore(TimeUnit.DAYS.toSeconds(20L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(20L)));

        Assert.assertEquals(18, feature.calculateScore(TimeUnit.DAYS.toSeconds(21L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(21L)));

        Assert.assertEquals(45, feature.calculateScore(TimeUnit.DAYS.toSeconds(50L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(50L)));

        Assert.assertEquals(71, feature.calculateScore(TimeUnit.DAYS.toSeconds(79L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(79L)));

        Assert.assertEquals(72, feature.calculateScore(TimeUnit.DAYS.toSeconds(80L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(80L)));

        Assert.assertEquals(72, feature.calculateScore(TimeUnit.DAYS.toSeconds(81L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(81L)));

        Assert.assertEquals(90, feature.calculateScore(TimeUnit.DAYS.toSeconds(100L)).intValue());
        Assert.assertEquals(90, feature.calculateScore(TimeUnit.DAYS.toSeconds(100L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(100L)));

        Assert.assertEquals(91, feature.calculateScore(TimeUnit.DAYS.toSeconds(101L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(101L)));

        Assert.assertEquals(92, feature.calculateScore(TimeUnit.DAYS.toSeconds(200L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(200L)));

        feature = DevProductivityProfile.Feature.builder()
                .name("abc")
                .maxValue(100L).featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                .lowerLimitPercentage(20).upperLimitPercentage(80)
                .build();

        Assert.assertNull(feature.calculateScore(null));
        Assert.assertNull(feature.calculateRating(null));

        Assert.assertEquals(90, feature.calculateScore(0L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(0L));

        Assert.assertEquals(90, feature.calculateScore(10L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(10L));

        Assert.assertEquals(81, feature.calculateScore(19L).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(19L));

        Assert.assertEquals(80, feature.calculateScore(20L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(20L));

        Assert.assertEquals(79, feature.calculateScore(21L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(21L));

        Assert.assertEquals(50, feature.calculateScore(50L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(50L));

        Assert.assertEquals(21, feature.calculateScore(79L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(79L));

        Assert.assertEquals(20, feature.calculateScore(80L).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(80L));

        Assert.assertEquals(19, feature.calculateScore(81L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(81L));

        Assert.assertEquals(0, feature.calculateScore(100L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(100L));

        Assert.assertEquals(0, feature.calculateScore(101L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(101L));

        Assert.assertEquals(0, feature.calculateScore(110L).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(110L));

        feature = DevProductivityProfile.Feature.builder()
                .name("abc")
                .maxValue(100L).maxUnit(TimeUnit.DAYS).featureType(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME)
                .lowerLimitPercentage(20).upperLimitPercentage(80)
                .build();

        Assert.assertNull(feature.calculateScore(null));
        Assert.assertNull(feature.calculateRating(null));

        Assert.assertEquals(90, feature.calculateScore(TimeUnit.DAYS.toSeconds(0L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(0L)));

        Assert.assertEquals(90, feature.calculateScore(TimeUnit.DAYS.toSeconds(10L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(10L)));

        Assert.assertEquals(81, feature.calculateScore(TimeUnit.DAYS.toSeconds(19L)).intValue());
        Assert.assertEquals(GOOD, feature.calculateRating(TimeUnit.DAYS.toSeconds(19L)));

        Assert.assertEquals(80, feature.calculateScore(TimeUnit.DAYS.toSeconds(20L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(20L)));

        Assert.assertEquals(79, feature.calculateScore(TimeUnit.DAYS.toSeconds(21L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(21L)));

        Assert.assertEquals(50, feature.calculateScore(TimeUnit.DAYS.toSeconds(50L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(50L)));

        Assert.assertEquals(21, feature.calculateScore(TimeUnit.DAYS.toSeconds(79L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(79L)));

        Assert.assertEquals(20, feature.calculateScore(TimeUnit.DAYS.toSeconds(80L)).intValue());
        Assert.assertEquals(ACCEPTABLE, feature.calculateRating(TimeUnit.DAYS.toSeconds(80L)));

        Assert.assertEquals(19, feature.calculateScore(TimeUnit.DAYS.toSeconds(81L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(81L)));

        Assert.assertEquals(0, feature.calculateScore(TimeUnit.DAYS.toSeconds(100L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(100L)));

        Assert.assertEquals(0, feature.calculateScore(TimeUnit.DAYS.toSeconds(101L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(101L)));

        Assert.assertEquals(0, feature.calculateScore(TimeUnit.DAYS.toSeconds(110L)).intValue());
        Assert.assertEquals(NEEDS_IMPROVEMENT, feature.calculateRating(TimeUnit.DAYS.toSeconds(110L)));
    }

    @Test
    public void testGetProfileWeightsTotal() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("dev_prod/profiles/devprod_profile_1.json");
        DevProductivityProfile profile = MAPPER.readValue(serialized, DevProductivityProfile.class);
        Integer profileWeightsTotal = DevProductivityProfile.getProfileWeightsTotal(profile);
        Assert.assertEquals(24, profileWeightsTotal.intValue());
        Assert.assertEquals(0, DevProductivityProfile.getProfileWeightsTotal(DevProductivityProfile.builder().build()).intValue());
    }

    @Test
    public void testReleased() {
        List<DevProductivityProfile.FeatureType> legacyFeatureTypes = Arrays.asList(DevProductivityProfile.FeatureType.values()).stream()
                .filter(f -> !f.getDisplayText().startsWith("Sonar"))
                .filter(f -> !f.getDisplayText().equals("Bugs per 100 Lines of code"))
                .filter(f -> !f.getDisplayText().equals("PR Review Depth"))
                .distinct()
                .collect(Collectors.toList());

        List<DevProductivityProfile.FeatureType> releasedFeatureTypes = Arrays.asList(DevProductivityProfile.FeatureType.values()).stream()
                .filter(f -> f.isReleased())
                .collect(Collectors.toList());
        Assert.assertEquals(legacyFeatureTypes, releasedFeatureTypes);
    }
}