package io.levelops.notification.clients.msteams;

import io.levelops.notification.models.msteams.MSTeamsChannel;
import io.levelops.notification.models.msteams.MSTeamsChat;
import io.levelops.notification.models.msteams.MSTeamsChatMessage;
import io.levelops.notification.models.msteams.MSTeamsTeam;
import io.levelops.notification.models.msteams.MSTeamsUser;

import java.util.List;
import java.util.Optional;

public interface MSTeamsBotClient {

    MSTeamsUser getLoggedInUser() throws MSTeamsClientException;

    MSTeamsChat createOrGetChat(String toEmail, String fromEmail) throws MSTeamsClientException;

    MSTeamsChatMessage sendMessageToUser(String chatId, String data) throws MSTeamsClientException;

    MSTeamsChatMessage sendMessageToChannel(String teamId, String channelId, String text) throws MSTeamsClientException;

    List<MSTeamsTeam> getAllTeams() throws MSTeamsClientException;

    List<MSTeamsChannel> getChannels(String teamId) throws MSTeamsClientException;

    Optional<MSTeamsUser> getUser(String emailId) throws MSTeamsClientException;

}
