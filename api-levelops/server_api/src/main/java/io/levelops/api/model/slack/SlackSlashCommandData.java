package io.levelops.api.model.slack;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;

import java.util.List;

@lombok.Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackSlashCommandData.SlackSlashCommandDataBuilder.class)
public class SlackSlashCommandData {
    String commandName;
    SlackSlashCommandCallback callback;
    List<String> tenants;
}