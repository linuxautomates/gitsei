package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(
        builder = GithubUserGraphQlResponse.GithubUserGraphQlResponseBuilder.class,
        using = GithubUserGraphQlResponse.GithubUserGraphQlResponseDeserializer.class)
public class GithubUserGraphQlResponse implements GithubGraphQlPaginatedResponse {
    @JsonProperty("users")
    List<GithubUser> users;

    @JsonProperty("endCursor")
    String endCursor;

    public static class GithubUserGraphQlResponseDeserializer extends StdDeserializer<GithubUserGraphQlResponse> {

        public GithubUserGraphQlResponseDeserializer() {
            this(null);
        }

        public GithubUserGraphQlResponseDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public GithubUserGraphQlResponse deserialize(com.fasterxml.jackson.core.JsonParser jp, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            boolean hasNextPage = node.get("data").get("organization").get("membersWithRole").get("pageInfo").get("hasNextPage").asBoolean();
            String endCursor = null;
            if (hasNextPage) {
                endCursor = node.get("data").get("organization").get("membersWithRole").get("pageInfo").get("endCursor").asText();
            }
            ObjectMapper objectMapper = DefaultObjectMapper.get();

            ArrayList<GithubUser> users = new ArrayList<>();
            node.get("data").get("organization").get("membersWithRole").get("edges").forEach(n -> {
                var currentNode = n.get("node");
                List<String> emails = objectMapper.convertValue(currentNode.get("organizationVerifiedDomainEmails"), new TypeReference<List<String>>() {
                });
                String name = Optional.ofNullable(currentNode.get("name")).map(JsonNode::asText).orElse(null);
                GithubUser user = GithubUser.builder()
                        .login(currentNode.get("login").asText())
                        .name(name)
                        .type(GithubUser.OwnerType.USER)
                        .orgVerifiedDomainEmails(emails)
                        .build();
                users.add(user);
            });
            return GithubUserGraphQlResponse.builder()
                    .users(users)
                    .endCursor(endCursor)
                    .build();
        }
    }
}
