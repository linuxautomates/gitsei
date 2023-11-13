package io.levelops.controlplane.trigger.runnables;

import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.ingestion.integrations.slack.models.SlackChatMessageQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SlackChatMessageTrigger implements TriggerRunnable {

    private final TriggeredJobService triggeredJobService;

    @Autowired
    public SlackChatMessageTrigger(TriggeredJobService triggeredJobService) {
        this.triggeredJobService = triggeredJobService;
    }

    @Override
    public String getTriggerType() {
        return "slack_chat_message";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        Map metadata = (Map) trigger.getMetadata();
        String userEmail = (String) metadata.get("user_email");
        String text = (String) metadata.get("text");
        triggeredJobService.createTriggeredJob(trigger, false, CreateJobRequest.builder()
                .controllerName("SlackChatMessageController")
                .query(SlackChatMessageQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .recipients(List.of(userEmail))
                        .text(":stopwatch: *Trigger*:\n```" + trigger.toString() + "```\nMessage: `" + text + "`")
                        .build())
                .build());
    }

}
