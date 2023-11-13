package io.levelops.notification.clients.msteams;

import io.levelops.notification.models.msteams.MSTeamsChannel;
import io.levelops.notification.models.msteams.MSTeamsChat;
import io.levelops.notification.models.msteams.MSTeamsChatMessage;
import io.levelops.notification.models.msteams.MSTeamsTeam;
import io.levelops.notification.models.msteams.MSTeamsUser;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Log4j2
public class CachedMSTeamsBotClient implements MSTeamsBotClient {

    private final MSTeamsBotClient delegate;

    @Builder
    public CachedMSTeamsBotClient(MSTeamsBotClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public MSTeamsUser getLoggedInUser() throws MSTeamsClientException {
        return delegate.getLoggedInUser();
    }

    @Override
    public MSTeamsChat createOrGetChat(String toEmail, String fromEmail) throws MSTeamsClientException {
        return delegate.createOrGetChat(toEmail, fromEmail);
    }

    @Override
    public MSTeamsChatMessage sendMessageToUser(String chatId, String message) throws MSTeamsClientException {
        return delegate.sendMessageToUser(chatId, message);
    }

    @Override
    public MSTeamsChatMessage sendMessageToChannel(String teamId, String channelId, String text) throws MSTeamsClientException {
        return delegate.sendMessageToChannel(teamId, channelId, text);
    }


    @Override
    public List<MSTeamsTeam> getAllTeams() throws MSTeamsClientException {
        return delegate.getAllTeams();
    }

    @Override
    public List<MSTeamsChannel> getChannels(String teamId) throws MSTeamsClientException {
        return delegate.getChannels(teamId);
    }

    @Override
    public Optional<MSTeamsUser> getUser(String emailId) throws MSTeamsClientException {
        return delegate.getUser(emailId);
    }

    private static <T> T handleCacheException(Callable<T> cacheAccess) throws MSTeamsClientException {
        try {
            return cacheAccess.call();
        } catch (Exception e) {
            if (e.getCause() instanceof MSTeamsClientException) {
                throw (MSTeamsClientException) e.getCause();
            } else {
                throw new MSTeamsClientException(e);
            }
        }
    }

}
