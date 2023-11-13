package io.levelops.integrations.github.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.graphql.GraphQlResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubUser;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubUserGraphQlResponseTest {
    @Test
    public void testDeserialize() {
        var graphQlResponse = GraphQlResponse.builder()
                .data(Map.of("organization", Map.of(
                        "membersWithRole", Map.of(
                                "totalCount", 2,
                                "edges", List.of(
                                        Map.of("node", Map.of(
                                                "login", "sid-propelo",
                                                "name", "Sid",
                                                "organizationVerifiedDomainEmails", List.of("sid@harness.io"))
                                        ),
                                        Map.of("node", Map.of(
                                                "login", "maxime-propelo",
                                                "name", "Maxime",
                                                "organizationVerifiedDomainEmails", List.of("maxime@harness.io"))
                                        )
                                ),
                                "pageInfo", Map.of(
                                        "endCursor", "Y3Vyc29yOjE=",
                                        "hasNextPage", true
                                )
                        )))).build();
        ObjectMapper mapper = DefaultObjectMapper.get();
        var users = mapper.convertValue(graphQlResponse, GithubUserGraphQlResponse.class);
        assertThat(users.getUsers().get(0)).isEqualTo(GithubUser.builder()
                .login("sid-propelo")
                .name("Sid")
                .orgVerifiedDomainEmails(List.of("sid@harness.io"))
                .type(GithubUser.OwnerType.USER)
                .build());
        assertThat(users.getUsers().get(1)).isEqualTo(GithubUser.builder()
                .login("maxime-propelo")
                .name("Maxime")
                .orgVerifiedDomainEmails(List.of("maxime@harness.io"))
                .type(GithubUser.OwnerType.USER)
                .build());
        assertThat(users.getEndCursor()).isEqualTo("Y3Vyc29yOjE=");
    }
}