package io.levelops.notification.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.notification.clients.msteams.MSTeamsBotClient;
import io.levelops.notification.clients.msteams.MSTeamsBotClientFactory;
import io.levelops.notification.clients.msteams.MSTeamsClientException;
import io.levelops.notification.models.msteams.MSTeamsChannel;
import io.levelops.notification.models.msteams.MSTeamsChat;
import io.levelops.notification.models.msteams.MSTeamsTeam;
import io.levelops.notification.models.msteams.MSTeamsUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class MSTeamsService {

    private final MSTeamsBotClientFactory msTeamsBotClientFactory;

    public MSTeamsService(MSTeamsBotClientFactory msTeamsBotClientFactory) {
        this.msTeamsBotClientFactory = msTeamsBotClientFactory;
    }

    public void postChatMessage(final IntegrationKey integrationKey, final String text, final String recipient) throws MSTeamsClientException {

        MSTeamsBotClient client = msTeamsBotClientFactory.get(integrationKey);

        // Get current User
        MSTeamsUser currentUser = client.getLoggedInUser();

        if(currentUser.getId() == null) {
            log.error("Unable to get Service Account details");
            throw new MSTeamsClientException("Unable to get Service Account details");
        }

        String userEmail = (currentUser.getMail() != null) ? currentUser.getMail() : currentUser.getUserPrincipalName();

        if (recipient != null && recipient.contains("/")) {
            // Send message in channel
            sendMessageInChannel(client, recipient, userEmail, text);
        } else {
            // Send message in chat
            sendMessageInChat(client, recipient, userEmail, text);
        }

    }

    private void sendMessageInChannel(MSTeamsBotClient client, String id, String currentUser, String message) throws MSTeamsClientException {

        String[] ids = id.split("/");
        String teamName = ids[0];
        String channelName = ids[1];

        // Get the team id from team name
        Optional<MSTeamsTeam> msTeams = client.getAllTeams().stream()
                .filter((team) -> team.getDisplayName().equals(teamName) || team.getId().equals(teamName))
                .findFirst();

        if (msTeams.isEmpty()) {
            log.error("Service Account is not in Team " + teamName);
            throw new MSTeamsClientException("Service Account is not in Team " + teamName);
        }
        String teamId = msTeams.get().getId();

        // Get the channel id from channel name
        Optional<MSTeamsChannel> msChannel = client.getChannels(teamId).stream()
                .filter((channel) -> channel.getDisplayName().equals(channelName) || channel.getId().equals(channelName))
                .findFirst();

        if (msChannel.isEmpty()) {
            log.error("Service Account is not part of Channel " + channelName);
            throw new MSTeamsClientException("Service Account is not part of Channel " + channelName);
        }
        String channelId = msChannel.get().getId();

        client.sendMessageToChannel(teamId, channelId, message);
    }

    private void sendMessageInChat(MSTeamsBotClient client, String recipientEmail,
                                   String userEmail, String message) throws MSTeamsClientException {

        // Get the Recipient User email
        Optional<MSTeamsUser> recipientUser = client.getUser(recipientEmail);
        if(recipientUser.isEmpty()){
            log.error("Can not found user: '" + recipientEmail + "'");
            throw new MSTeamsClientException("User not found " + recipientEmail);
        }
        String recipientUserEmail = (recipientUser.get().getMail() != null) ? recipientUser.get().getMail() : recipientUser.get().getUserPrincipalName();

        // Create Or Get the chat if not exists
        MSTeamsChat msTeamschat = client.createOrGetChat(recipientUserEmail, userEmail);
        if (msTeamschat != null) {

            // Send message to user
            client.sendMessageToUser(msTeamschat.getId(), message);
        } else {
            log.error("Chat creation failed between " + recipientEmail + " and " + userEmail);
            throw new MSTeamsClientException("Chat creation failed between " + recipientEmail + " and " + userEmail);
        }
    }

}
