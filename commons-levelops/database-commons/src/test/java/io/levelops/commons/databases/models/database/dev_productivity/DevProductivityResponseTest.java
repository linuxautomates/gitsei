package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DevProductivityResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        DevProductivityResponse expected = DevProductivityResponse.builder()
                .orgUserId(UUID.randomUUID()).fullName("Viraj Ajgaonkar").email("viraj@propelo.ai")
                .sectionResponses(List.of(
                        SectionResponse.builder()
                                .name("Section 1")
                                .description("Section 1 Description")
                                .order(0)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .sectionOrder(0)
                                                .name("Bugs Per 100 LOC")
                                                .description("Bugs Per 100 LOC")
                                                .order(0)
                                                .mean(77.00)
                                                .score(72)
                                                .rating(DevProductivityProfile.Rating.GOOD)
                                                .build(),
                                        FeatureResponse.builder()
                                                .sectionOrder(0)
                                                .name("Bugs Per 100 LOC")
                                                .description("Bugs Per 100 LOC")
                                                .order(1)
                                                .mean(77.00)
                                                .score(72)
                                                .rating(DevProductivityProfile.Rating.GOOD)
                                                .build()
                                )).build(),
                        SectionResponse.builder()
                                .name("Section 2")
                                .description("Section 2 Description")
                                .order(1)
                                .featureResponses(List.of(
                                        FeatureResponse.builder()
                                                .sectionOrder(0)
                                                .name("Bugs Per 100 LOC")
                                                .description("Bugs Per 100 LOC")
                                                .order(0)
                                                .mean(77.00)
                                                .score(72)
                                                .rating(DevProductivityProfile.Rating.GOOD)
                                                .build(),
                                        FeatureResponse.builder()
                                                .sectionOrder(0)
                                                .name("Bugs Per 100 LOC")
                                                .description("Bugs Per 100 LOC")
                                                .order(1)
                                                .mean(77.00)
                                                .score(72)
                                                .rating(DevProductivityProfile.Rating.GOOD)
                                                .build()
                                )).build()
                ))
                .interval(ReportIntervalType.LAST_MONTH).startTime(Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond()).endTime(Instant.now().getEpochSecond())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME.toString(), DevProductivityProfile.FeatureType.PRS_AVG_COMMENT_TIME.toString()))
                .resultTime(Instant.now().getEpochSecond())
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        DevProductivityResponse actual = MAPPER.readValue(serialized, DevProductivityResponse.class);
        Assert.assertEquals(expected, actual);
    }

}