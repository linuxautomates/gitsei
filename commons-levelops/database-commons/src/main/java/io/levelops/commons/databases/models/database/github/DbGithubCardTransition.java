package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.github.models.GithubCreator;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Optional;

@Getter
@Builder(toBuilder = true)
public class DbGithubCardTransition {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private final String integrationId;

    @JsonProperty("project_id")
    private final String projectId;

    @JsonProperty("card_id")
    private final String cardId;

    @JsonProperty("column_id")
    private final String columnId;

    @JsonProperty("updater")
    private final String updater;

    @JsonProperty("end_time")
    Long endTime;

    @JsonProperty("start_time")
    Long startTime;

    @JsonProperty("created_at")
    Long createdAt;

    public static DbGithubCardTransition fromGithubCardTransitionCreateEvent(String integrationId,
                                                                             GithubWebhookEvent event) {
        String projectId = getProjectId(event);
        String columnId = getColumnId(event);
        Long startTime = getTime(event);
        return DbGithubCardTransition.builder()
                .integrationId(integrationId)
                .projectId(projectId)
                .cardId(Optional.ofNullable(event.getProjectCard()).map(GithubProjectCard::getId).orElse(null))
                .columnId(columnId)
                .updater(Optional.ofNullable(event.getSender()).map(GithubCreator::getLogin).orElse(""))
                .startTime(startTime)
                .createdAt(new Date().toInstant().getEpochSecond())
                .build();
    }

    public static DbGithubCardTransition fromGithubCardTransitionMovedEvent(String integrationId,
                                                                            GithubWebhookEvent event) {
        String projectId = getProjectId(event);
        String fromColumnId = getFromColumnId(event);
        Long endTime = getTime(event);
        return DbGithubCardTransition.builder()
                .integrationId(integrationId)
                .projectId(projectId)
                .cardId(Optional.ofNullable(event.getProjectCard()).map(GithubProjectCard::getId).orElse(null))
                .columnId(fromColumnId)
                .updater(Optional.ofNullable(event.getSender()).map(GithubCreator::getLogin).orElse(""))
                .endTime(endTime)
                .createdAt(new Date().toInstant().getEpochSecond())
                .build();
    }

    public static DbGithubCardTransition fromGithubCardTransitionDeleteEvent(String integrationId,
                                                                             GithubWebhookEvent event) {
        String projectId = getProjectId(event);
        String columnId = getColumnId(event);
        Long endTime = getTime(event);
        return DbGithubCardTransition.builder()
                .integrationId(integrationId)
                .projectId(projectId)
                .cardId(Optional.ofNullable(event.getProjectCard()).map(GithubProjectCard::getId).orElse(null))
                .columnId(columnId)
                .updater(Optional.ofNullable(event.getSender()).map(GithubCreator::getLogin).orElse(""))
                .endTime(endTime)
                .createdAt(new Date().toInstant().getEpochSecond())
                .build();
    }

    @NotNull
    private static Long getTime(GithubWebhookEvent event) {
        return Optional.ofNullable(event.getProjectCard())
                .map(GithubProjectCard::getUpdatedAt)
                .map(time -> time.toInstant().getEpochSecond())
                .orElse(new Date().toInstant().getEpochSecond());
    }

    private static String getColumnId(GithubWebhookEvent event) {
        return Optional.ofNullable(event.getProjectCard()).map(GithubProjectCard::getColumnId).orElse(null);
    }

    private static String getFromColumnId(GithubWebhookEvent event) {
        return Optional.ofNullable(event.getChanges())
                .map(GithubWebhookEvent.GithubWebhookChanges::getColumnId)
                .map(GithubWebhookEvent.GithubWebhookChanges.Column::getFrom)
                .orElse(null);
    }

    private static String getProjectId(GithubWebhookEvent event) {
        String projectUrl = Optional.ofNullable(event.getProjectCard())
                .map(GithubProjectCard::getProjectUrl)
                .orElse(null);
        String projectId = "";
        if (StringUtils.isNotEmpty(projectUrl)) {
            projectId = projectUrl.substring(projectUrl.lastIndexOf("/")+1);
        }
        return projectId;
    }
}
