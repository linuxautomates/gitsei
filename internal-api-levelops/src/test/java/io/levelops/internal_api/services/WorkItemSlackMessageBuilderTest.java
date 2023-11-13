package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.WorkItem;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemSlackMessageBuilderTest {
    private static final String COMPANY = "test";
    private static final String APP_BASE_URL = "https://testui1.levelops.io";
    private static final List<String> STATES = List.of("CLOSED","OPEN","IN_REVIEW","NEW","Not Yet Reviewed","WIP");
    private static final String WI_ID = "5a94ff29-3556-43a7-97e2-e367da654d44";
    private static final String UPLOAD_ID = "22a3ce26-2178-4c73-bea9-c6b75728d1f7";

    @Test
    public void testWIGeneral() {
        WorkItem wi = WorkItem.builder()
                .id(WI_ID)
                .type(WorkItem.ItemType.MANUAL).ticketType(WorkItem.TicketType.REQUEST)
                .vanityId("KAFKA-123")
                .title("Create Kafka topic magic")
                .build();
        WorkItemSlackMessageBuilder builder = new WorkItemSlackMessageBuilder();
        WorkItemSlackMessageBuilder.WorkItemSlackMessages workItemSlackMessages = builder.buildInteractiveMessage(COMPANY, APP_BASE_URL, wi, STATES);
        String actual = workItemSlackMessages.getMessage();
        assertThat(actual).isEqualTo("[{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"<https://testui1.levelops.io/#/admin/workitems/details?workitem=KAFKA-123|Create Kafka topic magic>\"}},{ \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"Change ticket status\" }, \"accessory\": { \"type\": \"static_select\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"Select an item\", \"emoji\": true }, \"options\": [ { \"text\": { \"type\": \"plain_text\", \"text\": \"Closed\", \"emoji\": true }, \"value\": \"CLOSED\" },{ \"text\": { \"type\": \"plain_text\", \"text\": \"Open\", \"emoji\": true }, \"value\": \"OPEN\" },{ \"text\": { \"type\": \"plain_text\", \"text\": \"In Review\", \"emoji\": true }, \"value\": \"IN_REVIEW\" },{ \"text\": { \"type\": \"plain_text\", \"text\": \"New\", \"emoji\": true }, \"value\": \"NEW\" },{ \"text\": { \"type\": \"plain_text\", \"text\": \"Not Yet Reviewed\", \"emoji\": true }, \"value\": \"Not Yet Reviewed\" },{ \"text\": { \"type\": \"plain_text\", \"text\": \"WIP\", \"emoji\": true }, \"value\": \"WIP\" } ] } },{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"Forward this ticket\"},\"accessory\": {\"type\": \"multi_conversations_select\",\"placeholder\": {\"type\": \"plain_text\",\"text\": \"Select conversations\",\"emoji\": true}}},{\"block_id\":\"assign_snippet_ticket\",\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"`Assign To :`\"},\"accessory\":{\"action_id\":\"assignee_select_snippet_ticket\",\"type\":\"users_select\",\"placeholder\":{\"type\":\"plain_text\",\"text\":\"Select assignee\",\"emoji\":true}}}]");
        assertThat(workItemSlackMessages.getModalMessages()).isNull();
    }
}