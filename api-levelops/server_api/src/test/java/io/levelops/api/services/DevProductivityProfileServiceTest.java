package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.web.exceptions.BadRequestException;
import io.propelo.trellis_framework.client.TrellisAPIControllerClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DevProductivityProfileServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testValidateAndCreateDevProductivityProfile() {
        DevProductivityProfileService service = new DevProductivityProfileService(null, null, null);

        DevProductivityProfile devProductivityProfile = DevProductivityProfile.builder()
                .name("profile 1")
                .sections(List.of(
                        DevProductivityProfile.Section.builder().name("name 1").build(),
                        DevProductivityProfile.Section.builder().name("name 1").build()
                ))
                .settings(Map.of())
                .build();
        try {
            service.validateAndCreateDevProductivityProfile(devProductivityProfile);
            Assert.fail("BadRequestException expected");
        } catch (BadRequestException e) {
            Assert.assertTrue(e.getMessage().startsWith("Dev productivity profile contains duplicate section names"));
        }

        devProductivityProfile = DevProductivityProfile.builder()
                .name("profile 1").effortInvestmentProfileId(UUID.randomUUID())
                .sections(List.of(
                        DevProductivityProfile.Section.builder().name("name 1").enabled(true).weight(10)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder().name("feature 1").enabled(true).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH).build(),
                                        DevProductivityProfile.Feature.builder().name("feature 2").enabled(true).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH).build()
                                ))
                                .build(),
                        DevProductivityProfile.Section.builder().name("name 2").enabled(true).weight(10)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder().name("feature 3").enabled(true).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_STORIES_RESOLVED_PER_MONTH).build(),
                                        DevProductivityProfile.Feature.builder().name("feature 4").enabled(true).featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH).build()
                                ))
                                .build()
                ))
                .settings(Map.of())
                .build();
        try {
            service.validateAndCreateDevProductivityProfile(devProductivityProfile);
            Assert.fail("BadRequestException expected");
        } catch (BadRequestException e) {
            Assert.assertTrue(e.getMessage().startsWith("Dev productivity profile contains Investment profile but does not contain any categories"));
        }

        devProductivityProfile = DevProductivityProfile.builder()
                .name("profile 1")
                .sections(List.of(
                        DevProductivityProfile.Section.builder().name("name 1").enabled(true).weight(10).build(),
                        DevProductivityProfile.Section.builder().name("name 2").enabled(true).weight(null).build()
                ))
                .settings(Map.of())
                .build();
        try {
            service.validateAndCreateDevProductivityProfile(devProductivityProfile);
            Assert.fail("BadRequestException expected");
        } catch (BadRequestException e) {
            Assert.assertTrue(e.getMessage().startsWith("Dev productivity profile contains sections with null weights"));
        }
    }

}