package io.levelops.commons.databases.services.dev_productivity.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevProductivityFeatureHandler;
import io.levelops.commons.databases.services.dev_productivity.utils.OrgDevProductivityUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PERCENTAGE_OF_REWORK;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PRS_AVG_COMMENT_TIME;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE;
import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DevProductivityEngineTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String COMPANY = "test";
    private static int sectionOrder = 0;
    private static int featureOrder = 0;

    private static DevProductivityFeatureHandler handler = Mockito.mock(DevProductivityFeatureHandler.class);

    @Test
    public void testCalculateDevProductivity() throws IOException, SQLException {
        Mockito.doAnswer(invocation -> FeatureResponse.builder()
                .sectionOrder(sectionOrder++).order(featureOrder++).result(10l)
                .build()).when(handler).calculateFeature(anyString(), anyInt(), any(), anyMap(), any(), any(), anyMap(), any());

        Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> map = new HashMap<>();
        for(DevProductivityProfile.FeatureType ft : DevProductivityProfile.FeatureType.values()) {
            map.put(ft, handler);
        }
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/devprod_profile_1.json"), DevProductivityProfile.class);
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder().build();
        OrgUserDetails orgUserDetails = OrgUserDetails.builder().build();
        Map<String, Long> latestIngestedAtByIntegrationId = Map.of();


        DevProductivityEngine engine = new DevProductivityEngine(map);
        engine.calculateDevProductivity(COMPANY, profile, devProductivityFilter,
                orgUserDetails,
                latestIngestedAtByIntegrationId, null, null, null );
        verify(handler, times(19)).calculateFeature(any(), any(), any(), any(), any(), any(), any(), any());
        clearInvocations(handler);

        engine.calculateDevProductivity(COMPANY, profile, devProductivityFilter,
                orgUserDetails,
                latestIngestedAtByIntegrationId, null, null, Set.of() );
        verify(handler, times(19)).calculateFeature(any(), any(), any(), any(), any(), any(), any(), any());
        clearInvocations(handler);

        Set<DevProductivityProfile.FeatureType> featureTypes = Set.of(PRS_AVG_APPROVAL_TIME, PRS_AVG_COMMENT_TIME, PERCENTAGE_OF_REWORK,
                SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE, SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE, SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE);
        engine.calculateDevProductivity(COMPANY, profile, devProductivityFilter,
                orgUserDetails,
                latestIngestedAtByIntegrationId, null, null, featureTypes );
        verify(handler, times(3)).calculateFeature(any(), any(), any(), any(), any(), any(), any(), any());
        clearInvocations(handler);



    }
    @Test
    public void testCalculateWeightedSectionScore() {
        Assert.assertEquals(null, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(null, 100, 100));
        Assert.assertEquals(null, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(100, 100, null));
        Assert.assertEquals(null, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(100, 100, 0));
        Assert.assertEquals(0, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(100, 0, 100).intValue());

        Assert.assertEquals(41, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(91, 10, 22).intValue());
        Assert.assertEquals(11, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(80, 3, 22).intValue());
        Assert.assertEquals(9, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(67, 3, 22).intValue());
        Assert.assertEquals(16, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(58, 6, 22).intValue());

        Assert.assertEquals(2, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(4, 10, 22).intValue());
        Assert.assertEquals(6, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(45, 3, 22).intValue());
        Assert.assertEquals(0, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(0, 3, 22).intValue());
        Assert.assertEquals(14, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(51, 6, 22).intValue());

        Assert.assertEquals(25, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(56, 10, 22).intValue());
        Assert.assertEquals(8, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(60, 3, 22).intValue());
        Assert.assertEquals(9, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(67, 3, 22).intValue());
        Assert.assertEquals(0, DevProductivityEngine.ResponseHelper.calculateWeightedSectionScore(0, 6, 22).intValue());
    }

    @Test
    public void testOrgDevProductivityScoresCalculation() {
        DevProductivityProfile profile = DevProductivityProfile.builder()
                .sections(List.of(DevProductivityProfile.Section.builder()
                        .name("Volume")
                        .order(0)
                                .enabled(true)
                        .features(List.of(
                                DevProductivityProfile.Feature.builder()
                                        .name("Number of PRs")
                                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                        .enabled(true)
                                        .order(0)
                                        .maxValue(10l)
                                        .lowerLimitPercentage(25)
                                        .upperLimitPercentage(75)
                                        .build()
                        )).build())
                        ).build();
        List<DevProductivityResponse> orgUserresponses = List.of(DevProductivityResponse.builder()
                .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(4l)
                                                .count(12l)
                                                .build()
                                ))
                        .build())).build(),
                DevProductivityResponse.builder()
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(3l)
                                                .count(9l)
                                                .build()
                                ))
                                .build())).build(),
                DevProductivityResponse.builder()
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(0l)
                                                .count(0l)
                                                .build()
                                ))
                                .build())).build(),
                DevProductivityResponse.builder()
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(null)
                                                .count(null)
                                                .build()
                                ))
                                .build())).build());
        Map<Integer, Map<Integer,List<FeatureResponse>>> featureResponseMap =  OrgDevProductivityUtils.buildPartialFeatureResponses(orgUserresponses);
        DevProductivityResponse orgResponse = DevProductivityEngine.ResponseHelper.buildResponseFromPartialFeatureResponses(profile,featureResponseMap, ReportIntervalType.LAST_MONTH);
        Assert.assertNotNull(orgResponse);
        Assert.assertEquals(2,orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getResult().intValue());
        Assert.assertEquals(5,orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getCount().intValue());
        Assert.assertEquals("Number of PRs per month",orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getName());
        orgResponse = DevProductivityEngine.ResponseHelper.buildResponseFromPartialFeatureResponses(profile,featureResponseMap, ReportIntervalType.LAST_WEEK);
        Assert.assertEquals("Number of PRs in one week",orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getName());
    }

    @Test
    public void testOrgDevProductivityScoresCalculation2() {
        DevProductivityParentProfile parentProfile = DevProductivityParentProfile.builder()
                .name("parent-profile")
        .subProfiles(List.of(DevProductivityProfile.builder()
                        .name("Junior dev")
                                .order(0)
                                .enabled(true)
                .sections(List.of(DevProductivityProfile.Section.builder()
                        .name("Volume").weight(7)
                        .order(0)
                        .enabled(true)
                        .features(List.of(
                                DevProductivityProfile.Feature.builder()
                                        .name("Number of PRs")
                                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                        .enabled(true)
                                        .order(0)
                                        .maxValue(10l)
                                        .lowerLimitPercentage(25)
                                        .upperLimitPercentage(75)
                                        .build()
                        )).build(),
                        DevProductivityProfile.Section.builder()
                                .name("Impact").weight(8)
                                .order(1)
                                .enabled(true)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder()
                                                .name("High Impact stories")
                                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH)
                                                .enabled(true)
                                                .order(0)
                                                .maxValue(6l)
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build()
                                )).build())
                ).build(),
                DevProductivityProfile.builder()
                        .name("Senior dev")
                        .order(1)
                        .enabled(true)
                        .sections(List.of(DevProductivityProfile.Section.builder()
                                .name("Volume").weight(9)
                                .order(0)
                                .enabled(true)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder()
                                                .name("Number of PRs")
                                                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH)
                                                .enabled(true)
                                                .order(0)
                                                .maxValue(20l)
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build()
                                )).build(),
                                DevProductivityProfile.Section.builder()
                                        .name("Impact")
                                        .order(1).weight(6)
                                        .enabled(true)
                                        .features(List.of(
                                                DevProductivityProfile.Feature.builder()
                                                        .name("High Impact stories")
                                                        .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH)
                                                        .enabled(true)
                                                        .order(0)
                                                        .maxValue(8l)
                                                        .lowerLimitPercentage(25)
                                                        .upperLimitPercentage(75)
                                                        .build()
                                        )).build())
                        ).build(),
                DevProductivityProfile.builder()
                        .name("Senior QA")
                        .order(2)
                        .enabled(true)
                        .sections(List.of(DevProductivityProfile.Section.builder()
                                        .name("Quality").weight(5)
                                        .order(2)
                                        .enabled(true)
                                        .features(List.of(
                                                DevProductivityProfile.Feature.builder()
                                                        .name("Percentage of Rework")
                                                        .featureType(PERCENTAGE_OF_REWORK)
                                                        .enabled(true)
                                                        .order(0)
                                                        .maxValue(30l)
                                                        .lowerLimitPercentage(25)
                                                        .upperLimitPercentage(75)
                                                        .build()
                                        )).build())
                        ).build())).build();
        //4 org users - 2 Junior Dev  1 Senior Dev and 1 Senior QA
        List<DevProductivityResponse> orgUserresponses = List.of(DevProductivityResponse.builder()
                        .order(0)
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(4l)
                                                .count(12l)
                                                .build()
                                ))
                                .build(),
                                SectionResponse.builder()
                                        .order(1)
                                        .name("Impact")
                                        .enabled(true)
                                        .featureResponses(List.of(
                                                FeatureResponse.builder()
                                                        .order(0)
                                                        .name("High Impact stories")
                                                        .enabled(true)
                                                        .result(3l)
                                                        .count(9l)
                                                        .build()
                                        ))
                                        .build())).build(),
                DevProductivityResponse.builder()
                        .order(0)
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(null)
                                                .count(null)
                                                .build()
                                ))
                                .build(),
                                SectionResponse.builder()
                                        .order(1)
                                        .name("Impact")
                                        .enabled(true)
                                        .featureResponses(List.of(
                                                FeatureResponse.builder()
                                                        .order(0)
                                                        .name("High Impact stories")
                                                        .enabled(true)
                                                        .result(2l)
                                                        .count(6l)
                                                        .build()
                                        ))
                                        .build())).build(),
                DevProductivityResponse.builder()
                        .order(1)
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(0)
                                .name("Volume")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Number of PRs")
                                                .enabled(true)
                                                .result(0l)
                                                .count(0l)
                                                .build()
                                ))
                                .build(),
                                SectionResponse.builder()
                                        .order(1)
                                        .name("Impact")
                                        .enabled(true)
                                        .featureResponses(List.of(
                                                FeatureResponse.builder()
                                                        .order(0)
                                                        .name("High impact stories")
                                                        .enabled(true)
                                                        .result(1l)
                                                        .count(3l)
                                                        .build()
                                        ))
                                        .build())).build(),
                DevProductivityResponse.builder()
                        .order(2)
                        .sectionResponses(List.of(SectionResponse.builder()
                                .order(2)
                                .name("Quality")
                                .enabled(true)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .order(0)
                                                .name("Percentage of Rework")
                                                .enabled(true)
                                                .result(20l)
                                                .count(20l)
                                                .build()
                                ))
                                .build())).build());
        Map<Integer,Map<Integer, Map<Integer,List<FeatureResponse>>>> profileFeatureResponseMap =  OrgDevProductivityUtils.buildPartialFeatureResponsesByProfiles(orgUserresponses);
        DevProductivityResponse orgResponse = DevProductivityEngine.ResponseHelper.buildResponseFromPartialFeatureResponses(parentProfile,profileFeatureResponseMap, ReportIntervalType.LAST_MONTH);
        Assert.assertNotNull(orgResponse);
        Assert.assertEquals(3,orgResponse.getSectionResponses().size());
        Assert.assertEquals(6,orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getCount().intValue());
        Assert.assertEquals("Number of PRs per month",orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getName());
        orgResponse = DevProductivityEngine.ResponseHelper.buildResponseFromPartialFeatureResponses(parentProfile,profileFeatureResponseMap, ReportIntervalType.LAST_WEEK);
        Assert.assertEquals("Number of PRs in one week",orgResponse.getSectionResponses().get(0).getFeatureResponses().get(0).getName());
    }
}