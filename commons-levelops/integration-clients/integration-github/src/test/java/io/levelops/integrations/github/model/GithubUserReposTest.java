package io.levelops.integrations.github.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.graphql.GraphQlResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubRepository;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubUserReposTest {
    @Test
    public void testDeserialize() {
        var graphQlResponse = GraphQlResponse.builder()
                .data(Map.of("user", Map.of(
                        "login", "sid-propelo",
                        "repositoriesContributedTo", Map.of(
                                "totalCount", 2,
                                "nodes", List.of(
                                        Map.of("nameWithOwner", "test-repo",
                                                "owner", Map.of("login", "sid-propelo")),
                                        Map.of("nameWithOwner", "test-repo-2",
                                                "owner", Map.of("login", "sid-propelo"))
                                ),
                                "pageInfo", Map.of(
                                        "endCursor", "Y3Vyc29yOjE=",
                                        "hasNextPage", true
                                )
                        )
                ))).build();
        ObjectMapper mapper = DefaultObjectMapper.get();
        var repos = mapper.convertValue(graphQlResponse, GithubUserRepos.class);
        assertThat(repos.getLogin()).isEqualTo("sid-propelo");
        assertThat(repos.getRepos().stream().map(GithubRepository::getFullName)).containsExactlyElementsOf(List.of("test-repo", "test-repo-2"));
        assertThat(repos.getRepos().stream().map(r -> r.getOwner().getLogin())).containsExactlyElementsOf(List.of("sid-propelo", "sid-propelo"));
        assertThat(repos.getEndCursor()).isEqualTo("Y3Vyc29yOjE=");

        var emptyGraphQlResponse= GraphQlResponse.builder()
                .data(Map.of("user", Map.of(
                        "login", "sid-propelo",
                        "repositoriesContributedTo", Map.of(
                                "totalCount", 2,
                                "nodes", List.of(),
                                "pageInfo", Map.of(
                                        "endCursor", "null",
                                        "hasNextPage", "false"
                                )
                        )
                ))).build();
        var emptyRepos = mapper.convertValue(emptyGraphQlResponse, GithubUserRepos.class);
        assertThat(emptyRepos.getLogin()).isEqualTo("sid-propelo");
        assertThat(emptyRepos.getRepos()).isEqualTo(List.of());
        assertThat(emptyRepos.getEndCursor()).isEqualTo(null);
    }

}