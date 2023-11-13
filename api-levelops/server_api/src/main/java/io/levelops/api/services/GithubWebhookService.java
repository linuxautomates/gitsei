package io.levelops.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionCreateEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionDeleteEvent;
import static io.levelops.commons.databases.models.database.github.DbGithubCardTransition.fromGithubCardTransitionMovedEvent;

@Log4j2
@Service
public class GithubWebhookService {

    private static final String SECRETS = "__secrets__";
    private static final String WEBHOOK_SECRETS = "webhook_secret";
    private static final String X_HUB_SIGNATURE_256 = "x-hub-signature-256";

    private final GithubAggService aggService;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();

    @Autowired
    public GithubWebhookService(GithubAggService aggService, InventoryService inventoryService) {
        this.aggService = aggService;
        this.inventoryService = inventoryService;
    }

    public String onEvent(String company, String integrationId, String event, Map<String, String> header)
            throws JsonProcessingException, InventoryException, BadRequestException {
        if (!computePayloadSignatureAndVerify(company, integrationId, event, header)) {
            log.error("onEvent : Signature mismatch, ignoring payload for company {} with integration_id {}",
                    company, integrationId);
            throw new BadRequestException("Signature mismatch");
        }
        GithubWebhookEvent githubWebhookEvent = objectMapper.readValue(event, GithubWebhookEvent.class);
        String res = "";
        if (StringUtils.isNotEmpty(githubWebhookEvent.getZen()) && githubWebhookEvent.getHookId() != null) {
            log.info("onEvent: Received a ping for webhook for company={} and integrationId={} with Id={}",
                    company, integrationId, githubWebhookEvent.getHookId());
            return res;
        }
        switch (githubWebhookEvent.getAction()) {
            case "created":
                DbGithubCardTransition create = fromGithubCardTransitionCreateEvent(integrationId, githubWebhookEvent);
                res = aggService.insertCardTransition(company, create);
                break;
            case "moved":
                DbGithubCardTransition to = fromGithubCardTransitionCreateEvent(integrationId, githubWebhookEvent);
                DbGithubCardTransition from = fromGithubCardTransitionMovedEvent(integrationId, githubWebhookEvent);
                res = aggService.insertCardTransition(company, to);
                aggService.updateCardTransition(company, from);
                break;
            case "deleted":
                DbGithubCardTransition delete = fromGithubCardTransitionDeleteEvent(integrationId, githubWebhookEvent);
                res = aggService.updateCardTransition(company, delete);
                break;
            default:
                log.error("onEvent : Unexpected value for event action: {}, for company {} with integration_id {}",
                        githubWebhookEvent.getAction(), company, integrationId);
                throw new BadRequestException("Unexpected value: " + githubWebhookEvent.getAction());
        }
        return res;
    }

    private boolean computePayloadSignatureAndVerify(String company, String integrationId,
                                                     String event, Map<String, String> header)
            throws InventoryException {
        String headerSignature = getSignatureFromHeader(header);
        try {
            String webhookSecret = getWebhookSecret(company, integrationId);
            if (webhookSecret == null) {
                log.error("computePayloadSignatureAndVerify: Signature mismatch for company {}" +
                        " integrationId {}", company, integrationId);
                return false;
            }
            String dataSignature = computeDataSignature(webhookSecret, event);
            return secureCompare(headerSignature.getBytes(), dataSignature.getBytes());
        } catch (InventoryException e) {
            log.error("computePayloadSignatureAndVerify: Error creating github secret for company {}," +
                    " integrationId {}", company, integrationId, e);
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("computePayloadSignatureAndVerify: Error creating github secret for company {}," +
                    " integrationId {}", company, integrationId, e);
            throw new InventoryException(e);
        }
    }

    private String computeDataSignature(String webhookSecret, String event) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac githubSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
        githubSha256.init(secretKey);
        return "sha256=" + Hex.encodeHexString(githubSha256.doFinal(event.getBytes()));
    }

    @SuppressWarnings("unchecked")
    private String getWebhookSecret(String company, String integrationId) throws InventoryException {
        Integration integration = inventoryService.getIntegration(company, integrationId);
        return Optional.ofNullable(integration.getMetadata())
                .map(map -> (Map<String, String>) map.get(SECRETS))
                .map(secret -> secret.get(WEBHOOK_SECRETS))
                .orElse(null);
    }

    /**
     * https://codahale.com/a-lesson-in-timing-attacks/
     * to prevent against timed attacks
     *
     * @return true if signatures are same
     */
    private boolean secureCompare(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private String getSignatureFromHeader(Map<String, String> header) {
        String headerSignature = null;
        if (header.containsKey(X_HUB_SIGNATURE_256)) {
            headerSignature = header.get(X_HUB_SIGNATURE_256);
        }
        return headerSignature;
    }
}
