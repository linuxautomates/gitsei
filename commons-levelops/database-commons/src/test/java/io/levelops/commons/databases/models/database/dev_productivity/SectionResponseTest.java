package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SectionResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        SectionResponse expected = SectionResponse.builder()
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
                )).build();

        String serialized = MAPPER.writeValueAsString(expected);
        SectionResponse actual = MAPPER.readValue(serialized, SectionResponse.class);
        Assert.assertEquals(expected, actual);
    }

}