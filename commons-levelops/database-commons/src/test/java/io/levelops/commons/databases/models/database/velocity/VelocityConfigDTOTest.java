package io.levelops.commons.databases.models.database.velocity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityConfigDTOTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final Long LOWER_LIMIT = TimeUnit.DAYS.toSeconds(10);
    private static final Long UPPER_LIMIT = TimeUnit.DAYS.toSeconds(30);

    @Test
    public void testEventType() {
        for(VelocityConfigDTO.EventType expected : VelocityConfigDTO.EventType.values()) {
            VelocityConfigDTO.EventType actual = VelocityConfigDTO.EventType.fromString(expected.toString());
            Assert.assertEquals(expected, actual);
        }

        Assert.assertEquals(null, VelocityConfigDTO.EventType.fromString(null));
        Assert.assertEquals(null, VelocityConfigDTO.EventType.fromString(""));
        Assert.assertEquals(null, VelocityConfigDTO.EventType.fromString(" "));
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        Instant now = Instant.now();
        VelocityConfigDTO expected = VelocityConfigDTO.builder()
                .id(UUID.fromString("d67fb552-c8dd-463e-9ec9-7173e54f7ebc"))
                .name("Default Velocity").description("description").defaultConfig(true)
                //.createdAt(now).updatedAt(now)
                .preDevelopmentCustomStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Backlog Time").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("IN_PROGRESS")).build())
                                .build()
                ))
                .fixedStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Lead time to first commit").description("stage description").lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_COMMIT_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Dev Time").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Dev Time - PR Label Added").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED).params(Map.of("any_label_added", List.of("true"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Lead time to review").description("stage description").order(3).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Review Time").description("stage description").order(4).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_APPROVED).params(Map.of("last_approval", List.of("true"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Merge Time").description("stage description").order(5).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_MERGED).build())
                                .build()

                ))
                .postDevelopmentCustomStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Deploy to Staging").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(List.of("c67fb552-c8dd-463e-9ec9-7173e54f7ebd","01743634-d322-4f05-b765-db8902d8f7a0")).params(Map.of("branch", List.of("dev","staging"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("QA").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.WORKITEM_STATUS).values(List.of("QA", "Testing")).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Deploy to Prod").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(List.of("c67fb552-c8dd-463e-9ec9-7173e54f7ebd","01743634-d322-4f05-b765-db8902d8f7a0")).params(Map.of("branch", List.of("main","master"))).build())
                                .build()


                ))
                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        VelocityConfigDTO actual = MAPPER.readValue(serialized, VelocityConfigDTO.class);
        Assert.assertEquals(expected, actual);

        Map<UUID, String> cicdIdAndJobNameMapping = Map.of(
                UUID.fromString("c67fb552-c8dd-463e-9ec9-7173e54f7ebd"), "Stg Deploy",
                UUID.fromString("01743634-d322-4f05-b765-db8902d8f7a0"), "Prod Deploy"
        );

        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        stages.addAll(expected.getPreDevelopmentCustomStages());
        stages.addAll(expected.getFixedStages());
        stages.addAll(expected.getPostDevelopmentCustomStages());
        for(VelocityConfigDTO.Stage c : stages) {
            String builtDescription = c.getEvent().buildEventDescription(cicdIdAndJobNameMapping);
            Assert.assertNotNull(builtDescription);
        }
    }

    @Test
    public void testIsAnyLabelAdded() {
        VelocityConfigDTO.Event event = VelocityConfigDTO.Event.builder()
                .type(VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED).params(Map.of("any_label_added", List.of("true")))
                .build();
        Assert.assertTrue(event.isAnyLabelAdded());
        Assert.assertEquals("Label added to Pull Request", event.buildEventDescription(null));

        event = VelocityConfigDTO.Event.builder()
                .type(VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED).values(List.of("lbl_1", "lbl_2"))
                .build();
        Assert.assertFalse(event.isAnyLabelAdded());
        Assert.assertEquals("lbl_1,lbl_2 label added to Pull Request", event.buildEventDescription(null));

        event = VelocityConfigDTO.Event.builder()
                .type(VelocityConfigDTO.EventType.SCM_PR_CREATED).params(Map.of("any_label_added", List.of("true")))
                .build();
        Assert.assertFalse(event.isAnyLabelAdded());
        Assert.assertEquals("Pull Request Created", event.buildEventDescription(null));
    }
}