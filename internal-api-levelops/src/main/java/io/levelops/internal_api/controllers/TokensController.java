package io.levelops.internal_api.controllers;

import com.google.common.collect.Sets;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.token_services.AzureDevopsTokenService;
import io.levelops.commons.token_services.BitbucketTokenService;
import io.levelops.commons.token_services.BlackDuckTokenService;
import io.levelops.commons.token_services.CxSastTokenService;
import io.levelops.commons.token_services.GitlabTokenService;
import io.levelops.commons.token_services.MSTeamsTokenService;
import io.levelops.commons.token_services.SalesforceTokenService;
import io.levelops.commons.token_services.SlackTokenService;
import io.levelops.commons.token_services.TokenService;
import io.levelops.commons.token_services.TokenService.Tokens;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.internal_api.services.IntegrationSecretsService;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/integrations/{integrationid:[0-9]+}/tokens")
public class TokensController {
    private final TokenDataService tokenDataService;
    private final IntegrationService integrationService;
    private final SlackTokenService slackTokenService;
    private final MSTeamsTokenService msTeamsTokenService;
    private final BitbucketTokenService bitbucketTokenService;
    private final SalesforceTokenService salesForceTokenService;
    private final GitlabTokenService gitlabTokenService;
    private final AzureDevopsTokenService azureDevopsTokenService;
    private final CxSastTokenService cxSastTokenService;
    private final BlackDuckTokenService blackDuckTokenService;
    private final IntegrationSecretsService integrationSecretsService;
    private final GithubAppTokenService githubAppTokenService;
    private final LockRegistry tokenRefreshLockRegistry;
    private final int tokenRefreshLockWaitSeconds;
    private final boolean writeLegacyTokens; // feature flag - if false, turns off writing secrets to legacy tokens tables (ON by default)
    private final Boolean readTokensFromSecretsManagerService; // feature flag - turns on SM reads for ALL integrations (OFF by default)
    private final Map<String, Set<String>> useSecretsManagerServiceForIntegrations; // partial feature flag - only turns on SM for specified integrations (even if main flag is OFF)

    @Autowired
    public TokensController(TokenDataService tokenService,
                            IntegrationService integrationService,
                            SlackTokenService slackTokenService,
                            MSTeamsTokenService msTeamsTokenService,
                            BitbucketTokenService bitbucketTokenService,
                            SalesforceTokenService salesForceTokenService,
                            GitlabTokenService gitlabTokenService,
                            AzureDevopsTokenService azureDevopsTokenService,
                            CxSastTokenService cxSastTokenService,
                            BlackDuckTokenService blackDuckTokenService,
                            IntegrationSecretsService integrationSecretsService,
                            GithubAppTokenService githubAppTokenService,
                            @Qualifier("tokenRefreshLockRegistry") LockRegistry tokenRefreshLockRegistry,
                            @Value("${TOKEN_REFRESH_LOCK_WAIT_SECONDS:600}") Integer tokenRefreshLockWaitSeconds,
                            @Value("${WRITE_LEGACY_TOKENS:true}") boolean writeLegacyTokens,
                            @Value("${READ_TOKENS_FROM_SECRETS_MANAGER_SERVICE:false}") boolean readTokensFromSecretsManagerService,
                            @Value("#{'${USE_SECRETS_MANAGER_SERVICE_FOR_INTEGRATIONS:}'.split(',')}") // pass comma-list of "{tenant}@{integrationId}"
                                    List<String> useSecretsManagerServiceForIntegrations) {
        this.tokenDataService = tokenService;
        this.integrationService = integrationService;
        this.slackTokenService = slackTokenService;
        this.msTeamsTokenService = msTeamsTokenService;
        this.bitbucketTokenService = bitbucketTokenService;
        this.salesForceTokenService = salesForceTokenService;
        this.gitlabTokenService = gitlabTokenService;
        this.azureDevopsTokenService = azureDevopsTokenService;
        this.cxSastTokenService = cxSastTokenService;
        this.blackDuckTokenService = blackDuckTokenService;
        this.integrationSecretsService = integrationSecretsService;
        this.githubAppTokenService = githubAppTokenService;
        this.tokenRefreshLockRegistry = tokenRefreshLockRegistry;
        this.tokenRefreshLockWaitSeconds = tokenRefreshLockWaitSeconds;
        this.writeLegacyTokens = writeLegacyTokens;
        log.info("Writing secrets to legacy tokens tables is {}", writeLegacyTokens ? "ON" : "OFF");
        this.readTokensFromSecretsManagerService = readTokensFromSecretsManagerService;
        log.info("Reads from secrets manager service is {}", readTokensFromSecretsManagerService ? "ON" : "OFF");
        this.useSecretsManagerServiceForIntegrations = parseListOfTenantAtIntegrationIds(useSecretsManagerServiceForIntegrations);
        log.info("Read/Write to the secrets manager service has been enabled for the following integrations: {}", useSecretsManagerServiceForIntegrations);
    }

    private Token doGetToken(String company, String integrationId, String tokenId) throws SecretsManagerServiceClientException, SQLException, NotFoundException, AtlassianConnectServiceClientException {
        Optional<Token> tokenOpt;
        if (readTokensFromSecretsManagerService(company, integrationId)) {
            String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be discovered.
            tokenOpt = integrationSecretsService.get(company, smConfigId, integrationId);
        } else {
            tokenOpt = tokenDataService.get(company, tokenId);
        }
        Token token = tokenOpt.orElseThrow(() -> new NotFoundException("Could not refresh token: not found for id=" + tokenId + " company=" + company + " integrationId=" + integrationId));
        if (!OauthToken.TOKEN_TYPE.equalsIgnoreCase(token.getTokenData().getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Integration is not using OAuth for company=" + company + " integrationId=" + integrationId);
        }
        return token;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{tokenid:[0-9]+}/refresh", produces = "application/json")
    public DeferredResult<ResponseEntity<Token>> refreshIntegrationToken(@PathVariable("company") String company,
                                                                         @PathVariable("integrationid") String integrationId,
                                                                         @PathVariable("tokenid") String tokenId) {
        return SpringUtils.deferResponse(() -> {
            log.info("Refreshing token for company={}, integrationId={}", company, integrationId);
            Token tokenBeforeLock = doGetToken(company, integrationId, tokenId);
            Lock lock = tokenRefreshLockRegistry.obtain(company + integrationId + tokenId);
            if (!lock.tryLock(tokenRefreshLockWaitSeconds, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Could not acquire a lock to refresh token; wait or try again later. integrationId=" + integrationId + " for company=" + company);
            }
            String currentAccessTokenHash = null;
            try {
                Token token = doGetToken(company, integrationId, tokenId);
                // compare tokens: if different, then it was already refreshed
                String accessTokenBeforeLock = ((OauthToken) tokenBeforeLock.getTokenData()).getToken();
                String accessTokenBeforeLockHash = accessTokenBeforeLock != null? DigestUtils.md5Hex(accessTokenBeforeLock) : "null";
                String currentAccessToken = ((OauthToken) tokenBeforeLock.getTokenData()).getToken();
                currentAccessTokenHash = currentAccessToken != null? DigestUtils.md5Hex(currentAccessToken) : "null";
                if (currentAccessToken != null && !currentAccessToken.equals(accessTokenBeforeLock)) {
                    log.info("Token already refreshed for company={}, integrationId={}, tokenHash={}, tokenHashBeforeLock={}", company, integrationId,
                            currentAccessTokenHash, accessTokenBeforeLockHash);
                    return ResponseEntity.ok(token);
                }

                Integration it = integrationService.get(company, integrationId)
                        .orElseThrow(() -> new NotFoundException("Could not find integration id=" + integrationId + " for company=" + company));
                Map<String, Object> metadata = MapUtils.emptyIfNull(it.getMetadata());

                OauthToken dbTokens = (OauthToken) token.getTokenData();
                Long currentTimeInSeconds = System.currentTimeMillis() / 1000;
                Token tokenToReturn = token;
                //throttle token refreshes to once every 45 mins.
                if (dbTokens.getRefreshedAt() != null &&
                        (currentTimeInSeconds - dbTokens.getRefreshedAt()) <= TimeUnit.MINUTES.toSeconds(45)) {
                    log.info("Token was last refreshed at {} (less than 45 min ago) for company={}, integration={}, tokenHash={}",
                            DateUtils.fromEpochSecond(dbTokens.getRefreshedAt()), company, integrationId, currentAccessTokenHash);
                    return ResponseEntity.accepted().body(tokenToReturn);
                }
                Tokens tokens;
                switch (it.getApplication()) {
                    case "ms_teams":
                        tokens = msTeamsTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "slack":
                        tokens = slackTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "bitbucket":
                        tokens = bitbucketTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "salesforce":
                        tokens = salesForceTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "gitlab":
                        tokens = gitlabTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "azure_devops":
                        tokens = azureDevopsTokenService.getTokensFromRefreshToken(dbTokens.getRefreshToken());
                        break;
                    case "cxsast":
                        tokens = cxSastTokenService.generateToken(dbTokens.getRefreshToken(), it.getUrl());
                        break;
                    case "blackduck":
                        tokens = blackDuckTokenService.generateToken(dbTokens.getRefreshToken(), it.getUrl());
                        break;
                    case "github_actions":
                    case "github": {
                        // Note: GitHub Oauth does NOT require refresh - this is only for GitHub Apps
                        String appId = (String) metadata.get("app_id");
                        String installationId = (String) metadata.get("installation_id");
                        if (StringUtils.isAnyBlank(appId, installationId)) {
                            return ResponseEntity.accepted().body(tokenToReturn);
                        }
                        String accessToken = githubAppTokenService.generateAccessToken(it.getUrl(), appId, installationId, dbTokens.getRefreshToken());
                        tokens = Tokens.builder()
                                .accessToken(accessToken)
                                .refreshToken(dbTokens.getRefreshToken())
                                .build();
                        break;
                    }
                    default:
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                it.getApplication() + " does not have oauth token refresh implemented.");
                }
                OauthToken refreshedOauthToken = OauthToken.builder()
                        .refreshedAt(currentTimeInSeconds)
                        .token(StringUtils.defaultIfBlank(tokens.getAccessToken(), dbTokens.getToken()))
                        .refreshToken(StringUtils.defaultIfBlank(tokens.getRefreshToken(), dbTokens.getRefreshToken()))
                        .botToken(StringUtils.defaultIfBlank(tokens.getBotToken(), dbTokens.getBotToken()))
                        .build();
                tokenToReturn = Token.builder()
                        .integrationId(token.getIntegrationId())
                        .id(token.getId())
                        .createdAt(token.getCreatedAt())
                        .tokenData(refreshedOauthToken)
                        .build();
                Token tokenUpdate = Token.builder()
                        .id(tokenToReturn.getId())
                        .tokenData(tokenToReturn.getTokenData())
                        .build();
                String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be configurable. // TODO PHASE-2: store token-refresh SM-configuration in the integration
                integrationSecretsService.update(company, smConfigId, integrationId, tokenUpdate);
                if (writeLegacyTokens(company, integrationId)) {
                    tokenDataService.update(company, tokenUpdate);
                }
                log.info("Refreshed token successfully for company={}, integrationId={}, oldTokenHash={}, newTokenHash={}",
                        company, integrationId, DigestUtils.md5Hex(currentAccessToken), currentAccessTokenHash);
                return ResponseEntity.accepted().body(tokenToReturn);
            } catch (Exception e) {
                log.error("Failed to refresh token for company={}, integrationId={}, tokenHash={}", company, integrationId, currentAccessTokenHash);
                throw e;
            } finally {
                lock.unlock();
            }
        });
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createToken(@PathVariable("company") String company,
                                                                           @PathVariable("integrationid") String integrationId,
                                                                           @RequestBody Token token) {

        if (StringUtils.isEmpty(token.getIntegrationId())) {
            token = token.toBuilder()
                    .integrationId(integrationId)
                    .build();
        }
        final Token finalToken = token;
        return SpringUtils.deferResponse(() -> {
            String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be configurable.
            integrationSecretsService.insert(company, smConfigId, integrationId, finalToken);
            String result = "0";
            if (writeLegacyTokens(company, integrationId)) {
                result = tokenDataService.insert(company, finalToken);
            }
            return ResponseEntity.accepted().body(Map.of("token_id", result));
        });
    }


    @RequestMapping(method = RequestMethod.GET, value = "/{tokenid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Token>> getToken(@PathVariable("company") String company,
                                                          @PathVariable("integrationid") String integrationId,
                                                          @PathVariable("tokenid") String tokenId) {
        //using DeferredResult should effectively make function call async so tomcat can serve other requests.
        return SpringUtils.deferResponse(() -> {
            Optional<Token> tokenOpt;
            if (readTokensFromSecretsManagerService(company, integrationId)) {
                String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be discovered.
                tokenOpt = integrationSecretsService.get(company, smConfigId, integrationId);
            } else {
                tokenOpt = tokenDataService.get(company, tokenId);
            }
            return ResponseEntity.ok(tokenOpt
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find token with id=" + tokenId + " for company=" + company + " integration_id=" + integrationId)));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> deleteTokens(@PathVariable("company") String company,
                                                             @PathVariable("integrationid") String integrationId) {
        return SpringUtils.deferResponse(() -> {
            // For Phase 2 (supporting external SMs), we should figure out which secrets can be deleted (pre-existing secrets must NOT be deleted)
            integrationSecretsService.delete(company, integrationId);
            if (writeLegacyTokens(company, integrationId)) {
                tokenDataService.deleteByIntegration(company, integrationId);
            }
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<Token>>> listTokens(@PathVariable("company") String company,
                                                                            @PathVariable("integrationid") String integrationId,
                                                                            @RequestBody DefaultListRequest listRequest) {
        //using DeferredResult should effectively make function call async so tomcat can serve other requests.
        return SpringUtils.deferResponse(() -> {
            DbListResponse<Token> list;
            if (readTokensFromSecretsManagerService(company, integrationId)) {
                String smConfigId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // For Phase 2, this should be configurable.
                list = integrationSecretsService.get(company, smConfigId, integrationId)
                        .map(token -> DbListResponse.of(List.of(token), 1))
                        .orElse(DbListResponse.of(List.of(), 0));
            } else {
                list = tokenDataService.listByIntegration(company, integrationId, listRequest.getPage(), listRequest.getPageSize());
            }
            return ResponseEntity.ok(list);
        });
    }

    // region feature flags

    private boolean readTokensFromSecretsManagerService(String company, String integrationId) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        if (readTokensFromSecretsManagerService) {
            return true;
        }
        boolean read = useSecretsManagerServiceForIntegrations.getOrDefault(company.toLowerCase(), Set.of()).contains(integrationId.toLowerCase());
        if (read) {
            log.debug("Reads from secrets manager service have been turned ON for tenant={}, integrationId={}", company, integrationId);
        }
        return read;
    }

    private boolean writeLegacyTokens(String company, String integrationId) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        if (!writeLegacyTokens) { // if write is OFF, then we don't need to write for sure
            return false;
        }
        boolean useSM = useSecretsManagerServiceForIntegrations.getOrDefault(company.toLowerCase(), Set.of()).contains(integrationId.toLowerCase());
        if (useSM) {
            log.info("Writes to legacy tokens has been turned OFF for tenant={}, integrationId={}", company, integrationId);
            return false;
        }
        return true;
    }

    public static Map<String, Set<String>> parseListOfTenantAtIntegrationIds(List<String> readTokensFromSecretsManagerServiceForIntegrations) {
        return ListUtils.emptyIfNull(readTokensFromSecretsManagerServiceForIntegrations).stream()
                .filter(Objects::nonNull)
                .map(tenantIntegrationIdString -> {
                    String[] split = tenantIntegrationIdString.toLowerCase().trim().split("@");
                    return split.length != 2 ? null : Pair.of(split[0], split[1]);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getKey, pair -> Set.of(pair.getValue()), Sets::union));
    }

    // endregion

}
