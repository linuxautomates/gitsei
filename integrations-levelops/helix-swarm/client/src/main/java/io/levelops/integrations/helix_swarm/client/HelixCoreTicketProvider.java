package io.levelops.integrations.helix_swarm.client;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class HelixCoreTicketProvider {

    private static final Pattern URL_PATTERN = Pattern.compile("(?:(.*)://)?(.*):(.*)", Pattern.CASE_INSENSITIVE);
    private static final String SSL_ENABLED = "ssl_enabled";
    private static final String SSL_FINGERPRINT = "ssl_fingerprint";
    private static final String SSL_AUTO_ACCEPT = "ssl_auto_accept";
    private static final int MAX_ATTEMPTS = 5;

    private final String swarmUsername;
    private final String swarmPassword;
    private final Map<String, Object> metadata;
    private final String url;
    private final String integrationId;


    private String ticket;

    @Builder
    public HelixCoreTicketProvider(String swarmUsername, String swarmPassword,
                                   String ticket, Map<String, Object> metadata,
                                   String url, String integrationId) {
        this.swarmUsername = swarmUsername;
        this.swarmPassword = swarmPassword;
        this.ticket = ticket;
        this.metadata = metadata;
        this.url = url;
        this.integrationId = integrationId;
    }

    @Nullable
    public String getTicket() {
        return ticket;
    }

    @Nullable
    public String refreshTicket() {
        IOptionsServer server;
        try {
            server = getIOptionsServer(url, metadata);
        } catch (URISyntaxException | P4JavaException e) {
            log.error("Failed to create server for user " + swarmUsername + " for integrationId " +
                    integrationId + " " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        String ticket = server.getAuthTicket(swarmUsername);
        int count = 0;
        while (StringUtils.isEmpty(ticket) && count < MAX_ATTEMPTS) {
            ticket = server.getAuthTicket(swarmUsername);
            log.debug("Retry count attempt " + count + " to create server for user " + swarmUsername +
                    " for integration " + integrationId);
            count++;
        }
        if (StringUtils.isEmpty(ticket)) {
            log.error("Failed to generate ticket for helix core user " + swarmUsername + " for integration "
                    + integrationId);
            return null;
        }
        String updatedTicket = generateTokenFromTicket(ticket);
        this.ticket = updatedTicket;
        return updatedTicket;
    }

    private String generateTokenFromTicket(String ticket) {
        String toEncode = swarmUsername + ":" + ticket;
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    private IOptionsServer getIOptionsServer(String url, Map<String, Object> meta) throws URISyntaxException,
            P4JavaException {
        Map<String, Object> metadata = MapUtils.emptyIfNull(meta);
        Boolean sslEnabled = (Boolean) metadata.getOrDefault(SSL_ENABLED, false);
        Boolean sslAutoAccept = (Boolean) metadata.getOrDefault(SSL_AUTO_ACCEPT, false);
        String sslFingerprint = (String) metadata.getOrDefault(SSL_FINGERPRINT, null);

        IOptionsServer server = ServerFactory.getOptionsServer(ensureValidProtocol(url, sslEnabled), null);
        if (sslEnabled) {
            server.addTrust(sslFingerprint, new TrustOptions().setAutoAccept(sslAutoAccept));
        }
        server.connect();
        server.setUserName(swarmUsername);
        server.login(swarmPassword);
        return server;
    }

    private String ensureValidProtocol(String url, Boolean sslEnabled) {
        Matcher urlMatcher = URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            String protocol = urlMatcher.group(1);
            if (StringUtils.isEmpty(protocol)) {
                if (!sslEnabled)
                    return "p4java://" + url;
                else
                    return "p4javassl://" + url;
            } else if (protocol.equals("p4java") || protocol.equals("p4javassl")) {
                return url;
            }
        }
        return url;
    }
}
