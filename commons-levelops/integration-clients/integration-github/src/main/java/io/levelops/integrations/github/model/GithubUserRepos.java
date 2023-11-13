package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubUserRepos.GithubUserReposBuilder.class, using = GithubUserRepos.GithubUserReposDeserializer.class)
public class GithubUserRepos implements GithubGraphQlPaginatedResponse {
    @JsonProperty("login")
    String login;

    @JsonProperty("repos")
    List<GithubRepository> repos;

    @JsonProperty("endCursor")
    String endCursor;

    public static class GithubUserReposDeserializer extends StdDeserializer<GithubUserRepos> {

        public GithubUserReposDeserializer() {
            this(null);
        }

        public GithubUserReposDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public GithubUserRepos deserialize(com.fasterxml.jackson.core.JsonParser jp, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            boolean hasNextPage = node.get("data").get("user").get("repositoriesContributedTo").get("pageInfo").get("hasNextPage").asBoolean();
            String endCursor = null;
            if (hasNextPage) {
                endCursor = node.get("data").get("user").get("repositoriesContributedTo").get("pageInfo").get("endCursor").asText();
            }

            ArrayList<GithubRepository> repos = new ArrayList<>();
            node.get("data").get("user").get("repositoriesContributedTo").get("nodes").forEach(n -> {
                GithubRepository repo = GithubRepository.builder()
                        .fullName(n.get("nameWithOwner").asText())
                        .owner(GithubUser.builder()
                                .login(n.get("owner").get("login").asText())
                                .build())
                        .build();
                repos.add(repo);
            });
            return GithubUserRepos.builder()
                    .login(node.get("data").get("user").get("login").asText())
                    .repos(repos)
                    .endCursor(endCursor)
                    .build();
        }
    }
}
