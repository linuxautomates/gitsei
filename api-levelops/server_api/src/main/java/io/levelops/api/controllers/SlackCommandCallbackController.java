package io.levelops.api.controllers;

import io.levelops.api.model.slack.SlackSlashCommandCallback;
import io.levelops.api.model.slack.SlackSlashCommandData;
import io.levelops.api.services.SlackPropelCommandService;
import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.utils.ListUtils;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/webhooks/slack/commands")
public class SlackCommandCallbackController {

    private final String slackVerificationToken;
    private final SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    private final SlackPropelCommandService propelCommandService;

    @Autowired
    public SlackCommandCallbackController(
            @Value("${SLACK_VERIFICATION_TOKEN}") String slackVerificationToken,
            SlackTenantLookupDatabaseService slackTenantLookupDatabaseService,
            SlackPropelCommandService propelCommandService) {
        this.slackVerificationToken = slackVerificationToken;
        this.slackTenantLookupDatabaseService = slackTenantLookupDatabaseService;
        this.propelCommandService = propelCommandService;
    }

    @PostMapping(path = "/propel", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Object>>> receivePropelCommandEvent(
            @RequestBody String formDataString) throws IOException {
        return handleCommand("/propel", formDataString, propelCommandService::executePropelCommand);
    }

    // region generic code to handle commands

    private DeferredResult<ResponseEntity<Map<String, Object>>> handleCommand(String commandName,
                                                                 String formDataString,
                                                                 Function<SlackSlashCommandData, Map<String, Object>> handler) throws IOException {
        SlackSlashCommandCallback callback = parseFormData(commandName, formDataString);
        return SpringUtils.deferResponse(() -> {
            if (!validateCallback(callback)) {
                return ResponseEntity.ok().body(Map.of(
                        "response_type", "ephemeral",
                        "text", ":warning: Something went wrong..."));
            }
            List<String> tenants = lookupTenantIdsBySlackTeamId(callback.getTeamId());
            Map<String, Object> response = handler.apply(SlackSlashCommandData.builder()
                    .commandName(commandName)
                    .callback(callback)
                    .tenants(tenants)
                    .build());
            return ResponseEntity.ok().body(response);
        });
    }



    private SlackSlashCommandCallback parseFormData(String command, String formDataString) throws IOException {
        log.debug("Received Slack command: {} formData={}", command, formDataString);
        SlackSlashCommandCallback callback = SlackSlashCommandCallback.fromFormDataString(formDataString);
        log.info("Received Slack command: {} parsed={}", command, callback);
        return callback;
    }

    private boolean validateCallback(SlackSlashCommandCallback callback) {
        if (callback == null) {
            log.debug("Callback is null");
            return false;
        }
        if (!slackVerificationToken.equals(callback.getToken())) {
            log.error("Will not run command: Received mismatching token='{}'", callback.getToken());
            return false;
        }
        String teamId = callback.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            log.info("Cannot process Slack command, teamId is blank");
            return false;
        }
        return true;
    }

    private List<String> lookupTenantIdsBySlackTeamId(String teamId) throws SQLException {
        return ListUtils.emptyIfNull(slackTenantLookupDatabaseService.lookup(teamId))
                .stream().map(SlackTenantLookup::getTenantName)
                .collect(Collectors.toList());
    }
    // endregion
}
