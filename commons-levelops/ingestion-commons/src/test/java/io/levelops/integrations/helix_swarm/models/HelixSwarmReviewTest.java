package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class HelixSwarmReviewTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialization() throws IOException {
        String content = ResourceUtils.getResourceAsString("integrations/helix/helix_swarm_reviews.json");
        List<HelixSwarmReview> reviews = MAPPER.readValue(content, MAPPER.getTypeFactory().constructCollectionType(List.class, HelixSwarmReview.class));
        Assert.assertTrue(CollectionUtils.isNotEmpty(reviews));
        HelixSwarmReview expected = HelixSwarmReview.builder()
                .id(9L)
                .author("super")
                .changes(List.of(4L))
                .comments(List.of(0,0))
                .commits(List.of(4L))
                .commitStatus(List.of())
                .createdAt(1608801270L)
                .description("submit after edit\n")
                .groups(List.of("swarm-project-project1"))
                .participants(MAPPER.readTree(MAPPER.writeValueAsString(Map.of("super", List.of()))))
                .pending(false)
                .projects(MAPPER.readTree(MAPPER.writeValueAsString(List.of())))
                .state("needsReview")
                .stateLabel("Needs Review")
                .type("default")
                .updated(1608801270L)
                .updatedAt("2020-12-24T09:14:30+0000")
                .versions(List.of(HelixSwarmChange.builder()
                        .change(4l)
                        .user("super")
                        .time(1607084830l)
                        .pending(false)
                        .difference(1)
                        .addChangeMode("replace")
                        .stream("//JamCode/main")
                        .build()))
                .reviews(List.of(HelixSwarmActivity.builder()
                        .id(10l)
                        .action("requested")
                        .behalfOfExists(false)
                        .behalfOfFullName("")
                        .change(4l)
                        .comments(List.of(0l,0l))
                        .description("submit after edit\n")
                        .streams(MAPPER.readTree(MAPPER.writeValueAsString(List.of("review-9","user-super","personal-super"))))
                        .preposition("for")
                        .target("review 9 (revision 1)")
                        .time(1608801270l)
                        .topic("reviews/9")
                        .type("review")
                        .url("/reviews/9/v1/")
                        .user("super")
                        .userExists(true)
                        .userFullName("super")
                        .build()))
                .fileInfos(List.of(
                        ReviewFileInfo.builder().depotFile("//local/broadcom/test.txt").action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").build(),
                        ReviewFileInfo.builder().depotFile("//local/symantec/common/test.txt").action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").build(),
                        ReviewFileInfo.builder().depotFile("//local/symantec/consumer/endpoint/test.txt").action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").build(),
                        ReviewFileInfo.builder().depotFile("//local/symantec/enterprise/endpoint/test.txt").action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").build(),
                        ReviewFileInfo.builder().depotFile("//local/symantec/enterprise/network/test.txt").action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").build()
                ))
                .build();

        HelixSwarmReview actual = reviews.get(4);
        String s1 = MAPPER.writeValueAsString(expected);
        String s2 = MAPPER.writeValueAsString(actual);
        Assert.assertEquals(s1, s2);
        Assert.assertEquals(expected, actual);

        actual = reviews.get(0);
        Assert.assertEquals(968, actual.getId().longValue());
        Assert.assertEquals(true, actual.getReviewFilesApiNotFound());
    }

    @Test
    public void testParseReviewUpdateDate(){
        Assert.assertEquals(Instant.ofEpochSecond(1608801270l), HelixSwarmReview.parseReviewUpdateDate("2020-12-24T09:14:30+0000"));
        Assert.assertEquals(Instant.ofEpochSecond(1608801270l), HelixSwarmReview.parseReviewUpdateDate("2020-12-24T09:14:30+00:00"));
        Assert.assertEquals(null, HelixSwarmReview.parseReviewUpdateDate("401.E401E22"));
    }
}
