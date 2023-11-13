package io.levelops.ingestion.agent.ingestion;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.slack.models.SlackUserQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.slack.client.SlackBotClientFactory;
import io.levelops.integrations.slack.client.SlackClientException;
import io.levelops.integrations.slack.models.SlackApiUser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.UUID;

@Log4j2
public class SlackUserController implements DataController<SlackUserQuery> {
    private final ObjectMapper objectMapper;
    private final SlackBotClientFactory slackBotClientFactory;

    @Builder
    public SlackUserController(ObjectMapper objectMapper, SlackBotClientFactory slackBotClientFactory) {
        this.objectMapper = objectMapper;
        this.slackBotClientFactory = slackBotClientFactory;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, SlackUserQuery query) throws IngestException {
        log.info("Starting work on job id {}, query = {}", jobContext.getJobId(), query);

        SlackUserResult result = null;
        SlackUser slackUser = null;
        String slackUserId = query.getSlackUserId();
        IntegrationKey key = query.getIntegrationKey();
        try {
            boolean found = false;
            Optional<SlackApiUser> slackApiUserOpt = slackBotClientFactory.get(key).lookupUserById(slackUserId);
            if(slackApiUserOpt.isPresent()) {
                found = true;
                SlackApiUser slackApiUser = slackApiUserOpt.get();
                log.info("slackApiUser = {}", slackApiUser);
                slackUser = SlackUser.builder()
                        .teamId(slackApiUser.getTeamId())
                        .userId(slackApiUser.getId())
                        .realNameNormalized(slackApiUser.getProfile().getRealNameNormalized())
                        .username(slackApiUser.getName())
                        .email(slackApiUser.getProfile().getEmail())
                        .build();
                log.info("slackUser = {}", slackUser);
            }
            result = SlackUserResult.builder().success(true).found(found).slackUserId(slackUserId).workItemNoteId(query.getWorkItemNoteId()).slackUser(slackUser).build();

        } catch (SlackClientException e) {
            log.error("Error fetching user by user id {}", slackUserId);
            result = SlackUserResult.builder().success(false).found(false).slackUserId(slackUserId).workItemNoteId(query.getWorkItemNoteId()).slackUser(slackUser).build();
        }
        log.info("SlackUserResult = {}", result);
        return result;
    }


    @Override
    public SlackUserQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, SlackUserQuery.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackUserResult.SlackUserResultBuilder.class)
    public static class SlackUserResult implements ControllerIngestionResult {
        @JsonProperty("success")
        Boolean success;
        @JsonProperty("found")
        Boolean found;

        @JsonProperty("slack_user_id")
        String slackUserId;

        @JsonProperty("work_item_note_id")
        UUID workItemNoteId;

        @JsonProperty("slack_user")
        SlackUser slackUser;
    }
}
