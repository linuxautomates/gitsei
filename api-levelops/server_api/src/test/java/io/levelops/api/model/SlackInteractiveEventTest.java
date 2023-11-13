package io.levelops.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.slack.SlackInteractiveEvent;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SlackInteractiveEventTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testTicketChangeStatusEvent() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_interactive_event/ticket_change_status_event.json");
        SlackInteractiveEvent slackInteractiveEvent = MAPPER.readValue(serialized, SlackInteractiveEvent.class);
        Assert.assertNotNull(slackInteractiveEvent);
        Assert.assertEquals("Closed", slackInteractiveEvent.getActions().get(0).getSelectedOption().getValue());
    }

    @Test
    public void testTicketChangeAssigneeEvent() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_interactive_event/ticket_forward_event.json");
        SlackInteractiveEvent slackInteractiveEvent = MAPPER.readValue(serialized, SlackInteractiveEvent.class);
        Assert.assertNotNull(slackInteractiveEvent);
        Assert.assertEquals(List.of("US4JC7ZM5","G01BTBKV541"), slackInteractiveEvent.getActions().get(0).getSelectedConversations());
    }

    @Test
    public void testQuestionnaireAnswerInline() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_interactive_event/questionnaire_answer_inline_event.json");
        SlackInteractiveEvent slackInteractiveEvent = MAPPER.readValue(serialized, SlackInteractiveEvent.class);
        Assert.assertNotNull(slackInteractiveEvent);
        Assert.assertEquals("edit_questionnaire~@@@_foo~@@@_84949243-2468-45f7-ac73-c63247b3f74c", slackInteractiveEvent.getActions().get(0).getValue());
        Assert.assertEquals("Answer Inline", slackInteractiveEvent.getActions().get(0).getText().getText());
    }

    @Test
    public void testSubmitQuestionnaire() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_interactive_event/questionnaire_submit_event.json");
        SlackInteractiveEvent slackInteractiveEvent = MAPPER.readValue(serialized, SlackInteractiveEvent.class);
        Assert.assertNotNull(slackInteractiveEvent);
        Assert.assertEquals("view_submission", slackInteractiveEvent.getType());
        Assert.assertEquals("edit_questionnaire~@@@_foo~@@@_5fa8d4fe-ff0e-405b-a9d5-c820584798c9", slackInteractiveEvent.getView().getPrivateMetadata());
        Assert.assertEquals("edit_questionnaire", slackInteractiveEvent.getView().getCallbackId());
    }

    @Test
    public void testTicketViewTextAttachmentEvent() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_interactive_event/ticket_view_text_attachment_event.json");
        SlackInteractiveEvent slackInteractiveEvent = MAPPER.readValue(serialized, SlackInteractiveEvent.class);
        Assert.assertNotNull(slackInteractiveEvent);
        Assert.assertEquals("12466734323.1395872398", slackInteractiveEvent.getTriggerId());
        Assert.assertEquals(1, slackInteractiveEvent.getActions().size());
        SlackInteractiveEvent.Action action = slackInteractiveEvent.getActions().get(0);
        Assert.assertEquals("view_wi_text_attachment", action.getActionId());
        Assert.assertEquals("view_wi_text_attachment~@@@_test~@@@_5a94ff29-3556-43a7-97e2-e367da654d44~@@@_22a3ce26-2178-4c73-bea9-c6b75728d1f7", action.getValue());
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        SlackInteractiveEvent.ViewStateValueAction action1 = SlackInteractiveEvent.ViewStateValueAction.builder()
                .type("checkboxes")
                .selectedOptions(List.of(
                        SlackInteractiveEvent.SelectedOption.builder().value("option 1").build(),
                        SlackInteractiveEvent.SelectedOption.builder().value("option 2").build()
                ))
                .build();
        SlackInteractiveEvent.ViewStateValueAction action2 = SlackInteractiveEvent.ViewStateValueAction.builder()
                .type("checkboxes")
                .selectedOption(SlackInteractiveEvent.SelectedOption.builder().value("option 1").build())
                .build();
        SlackInteractiveEvent.ViewStateValueAction action3 = SlackInteractiveEvent.ViewStateValueAction.builder()
                .type("checkboxes")
                .value("test 1")
                .build();

        SlackInteractiveEvent.ViewState state = SlackInteractiveEvent.ViewState.builder()
                .values(Map.of(
                        "06db5640-49e4-4518-8f0b-8e9603766057_5ae2307f-2831-478d-920a-61de580b1c5d", SlackInteractiveEvent.ViewStateValue.builder().customActionId(action1).build(),
                        "06db5640-49e4-4518-8f0b-8e9603766057_47c9c67b-d7ab-4a63-8e7d-42dfb7342800", SlackInteractiveEvent.ViewStateValue.builder().customActionId(action2).build(),
                        "06db5640-49e4-4518-8f0b-8e9603766057_a412f1b0-a626-4f17-b8e7-56ec0affe359", SlackInteractiveEvent.ViewStateValue.builder().customActionId(action3).build()
                ))
                .build();

        SlackInteractiveEvent.View view = SlackInteractiveEvent.View.builder()
                .id("V01DQQKGB1N")
                .teamId("TM3U03S49")
                .type("modal")
                .privateMetadata("edit_questionnaire~@@@_foo~@@@_5fa8d4fe-ff0e-405b-a9d5-c820584798c9")
                .callbackId("edit_questionnaire")
                .state(state)
                .build();

        SlackInteractiveEvent event = SlackInteractiveEvent.builder()
                .view(view)
                .build();

        String serialized = MAPPER.writeValueAsString(event);
        Assert.assertNotNull(serialized);
    }

}