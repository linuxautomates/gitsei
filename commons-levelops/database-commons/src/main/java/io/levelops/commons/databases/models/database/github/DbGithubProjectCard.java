package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.github.models.GithubProjectCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbGithubProjectCard {

    @JsonProperty("id")
    private String id;

    @JsonProperty("current_column_id")
    private String currentColumnId;

    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("archived")
    private Boolean archived;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("content_url")
    private String contentUrl;

    @JsonProperty("issue_id")
    private String issueId;

    @JsonProperty("card_created_at")
    private Long cardCreatedAt;

    @JsonProperty("card_updated_at")
    private Long cardUpdatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    private String number;

    private String repoId;


    public static DbGithubProjectCard fromProjectCard(GithubProjectCard source, String currentColumnId) {
        DbGithubProjectCardBuilder dbGithubProjectCardBuilder = DbGithubProjectCard.builder()
                .currentColumnId(currentColumnId)
                .cardId(source.getId())
                .archived(source.getArchived())
                .creator(source.getCreator().getLogin())
                .contentUrl(source.getContentUrl())
                .cardCreatedAt(source.getCreatedAt().toInstant().getEpochSecond())
                .cardUpdatedAt(source.getUpdatedAt().toInstant().getEpochSecond());
        parseContentUrl(source, dbGithubProjectCardBuilder);
        return dbGithubProjectCardBuilder
                .build();
    }

    private static DbGithubProjectCardBuilder parseContentUrl(GithubProjectCard source, DbGithubProjectCardBuilder builder) {
        String contentUrl = source.getContentUrl();
        if (contentUrl == null || contentUrl.lastIndexOf("repos") == -1 || contentUrl.lastIndexOf("/") == -1)
            return builder.number(null).repoId(null);
        String number = contentUrl.substring(contentUrl.lastIndexOf("/") + 1);
        String repo = contentUrl.substring(contentUrl.lastIndexOf("repos") + 6, contentUrl.lastIndexOf("/") - 7);
        return builder.number(number).repoId(repo);
    }
}