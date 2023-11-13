package io.levelops.api.services;

import com.google.common.annotations.VisibleForTesting;
import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.api.config.IngestionConfig;
import io.levelops.api.exceptions.PreflightCheckFailedException;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.requests.ModifyIntegrationRequest;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.AtlassianConnectJwtToken;
import io.levelops.commons.databases.models.database.tokens.DBAuth;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.token_exceptions.TokenException;
import io.levelops.commons.token_services.AzureDevopsTokenService;
import io.levelops.commons.token_services.BitbucketTokenService;
import io.levelops.commons.token_services.BlackDuckTokenService;
import io.levelops.commons.token_services.CxSastTokenService;
import io.levelops.commons.token_services.GithubTokenService;
import io.levelops.commons.token_services.GitlabTokenService;
import io.levelops.commons.token_services.MSTeamsTokenService;
import io.levelops.commons.token_services.SalesforceTokenService;
import io.levelops.commons.token_services.ServicenowTokenService;
import io.levelops.commons.token_services.SlackTokenService;
import io.levelops.commons.token_services.TokenService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.MapUtils;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.github.models.GithubCreateWebhookQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.CreateTriggerRequest;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.models.PreflightCheckResults;
import io.levelops.services.PreflightCheckService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class IntegrationManagementService {
    private static final Set<IntegrationType> SUPPORTED_APP_TYPES = Set.of(
            IntegrationType.GITHUB,
            IntegrationType.JIRA,
            IntegrationType.CONFLUENCE,
            IntegrationType.SLACK,
            IntegrationType.MS_TEAMS,
            IntegrationType.SNYK,
            IntegrationType.PAGERDUTY,
            IntegrationType.BITBUCKET,
            IntegrationType.COVERITY,
            IntegrationType.POSTGRES,
            IntegrationType.SPLUNK,
            IntegrationType.ZENDESK,
            IntegrationType.SALESFORCE,
            IntegrationType.GERRIT,
            IntegrationType.OKTA,
            IntegrationType.TESTRAILS,
            IntegrationType.SONARQUBE,
            IntegrationType.GITLAB,
            IntegrationType.AZURE_DEVOPS,
            IntegrationType.CIRCLECI,
            IntegrationType.DRONECI,
            IntegrationType.HARNESSNG,
            IntegrationType.CUSTOM,
            IntegrationType.AWSDEVTOOLS,
            IntegrationType.HELIX_CORE,
            IntegrationType.HELIX_SWARM,
            IntegrationType.HELIX,
            IntegrationType.CXSAST,
            IntegrationType.JENKINS,
            IntegrationType.BITBUCKET_SERVER,
            IntegrationType.BLACKDUCK,
            IntegrationType.REPORT_PRAETORIAN,
            IntegrationType.REPORT_MS_TMT,
            IntegrationType.REPORT_NCCGROUP,
            IntegrationType.SAST_BRAKEMAN,
            IntegrationType.SERVICENOW,
            IntegrationType.GITHUB_ACTIONS
    );

    private static final Set<IntegrationType> SUPPRESSED_TRIGGER_INTEGRATIONS = Set.of(
            IntegrationType.SLACK,
            IntegrationType.MS_TEAMS,
            IntegrationType.POSTGRES,
            IntegrationType.SPLUNK,
            IntegrationType.CUSTOM,
            IntegrationType.JENKINS,
            IntegrationType.REPORT_PRAETORIAN,
            IntegrationType.REPORT_MS_TMT,
            IntegrationType.REPORT_NCCGROUP,
            IntegrationType.SAST_BRAKEMAN
    );

    private static final String DEFAULT_PRODUCT_KEY = "DEFAULT";

    private static final String SECRETS = "__secrets__";
    private static final String WEBHOOK_SECRET = "webhook_secret";
    private static final String IS_PUSH_BASED = "is_push_based";
    private static final String AUTO_REGISTER_WEBHOOK = "auto_register_webhook";
    private static final String WEBHOOK_CONTROLLER = "GithubWebhookController";

    private static final List<String> GITHUB_WEBHOOK_EVENTS = List.of("ping", "project", "project_column",
            "project_card", "issues", "pull_request", "pull_request_review", "push");

    private static final String ADO_SUBTYPE_METADATA_FIELD = "subtype";
    private static final String ADO_ENABLED_METADATA_FIELD = "enabled";

    @Getter
    @AllArgsConstructor
    public enum AdoSubtype {
        WI("wi", "Boards", Set.of("fetch_work_items", "fetch_workitem_fields", "fetch_metadata", "fetch_work_items_comments", "fetch_teams", "fetch_iterations", "fetch_workitem_histories")),
        SCM("scm", "Repos", Set.of("fetch_commits", "fetch_tags", "fetch_prs", "fetch_branches", "fetch_change_sets", "fetch_labels")),
        CICD("cicd", "Pipelines", Set.of("fetch_pipelines", "fetch_builds", "fetch_releases"));

        private final String metadataField;
        private final String displayName;
        private final Set<String> ingestionFlags;

        public Set<String> getIngestionFlagsComplement() {
            return Arrays.stream(values())
                    .filter(s -> !s.equals(this))
                    .map(AdoSubtype::getIngestionFlags)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }

        public String generateIntegrationName(String baseName) {
            return baseName + " - " + this.getDisplayName();
        }
    }

    private final String inventoryServiceUrl;
    private final String githubWebhookHandlingServiceUrl;
    private final PreflightCheckService preflightCheckService;
    private final InventoryService inventoryService;
    private final GithubTokenService githubTokenService;
    private final GitRepositoryService gitRepositoryService;
    private final BitbucketTokenService bitbucketTokenService;
    private final SlackTokenService slackTokenService;
    private final MSTeamsTokenService msTeamsTokenService;
    private final SalesforceTokenService salesForceTokenService;
    private final GitlabTokenService gitlabTokenService;
    private final AzureDevopsTokenService azureDevopsTokenService;
    private final CxSastTokenService cxSastTokenService;
    private final ControlPlaneService controlPlaneService;
    private final SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final BlackDuckTokenService blackDuckTokenService;
    private final ServicenowTokenService servicenowTokenService;
    private final GithubAppTokenService githubAppTokenService;
    private final IngestionConfig.IngestionTriggerSettings ingestionTriggerSettings;
    private final ProductService productService;
    private final ProductIntegMappingService productIntegMappingService;
    private final Set<IntegrationType> skipPreflightCheckIntegrations;
    private final Set<String> ingestionDisabledTenants;
    private final boolean allowUnsplitAdo;
    private final Set<IntegrationType> ingestionCallbackDisabledApps;
    private final AtlassianConnectServiceClient atlassianConnectServiceClient;

    @Autowired
    public IntegrationManagementService(InventoryService inventoryService,
                                        GithubTokenService githubTokenService,
                                        GitRepositoryService gitRepositoryService,
                                        @Qualifier("githubWebhookHandlingServiceUrl") String githubWebhookHandlingServiceUrl,
                                        BitbucketTokenService bitbucketTokenService,
                                        GitlabTokenService gitlabTokenService,
                                        AzureDevopsTokenService azureDevopsTokenService,
                                        @Qualifier("inventoryServiceUrl") String inventoryUrl,
                                        SlackTokenService slackTokenService,
                                        MSTeamsTokenService msTeamsTokenService,
                                        SalesforceTokenService salesForceTokenService,
                                        ControlPlaneService controlPlaneService,
                                        PreflightCheckService preflightCheckService,
                                        SlackTenantLookupDatabaseService slackTenantLookupDatabaseService,
                                        CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
                                        CxSastTokenService cxSastTokenService,
                                        BlackDuckTokenService blackDuckTokenService,
                                        ServicenowTokenService servicenowTokenService,
                                        GithubAppTokenService githubAppTokenService,
                                        ProductService productService,
                                        ProductIntegMappingService productIntegMappingService,
                                        IngestionConfig.IngestionTriggerSettings ingestionTriggerSettings,
                                        AtlassianConnectServiceClient atlassianConnectServiceClient,
                                        @Value("${SKIP_PREFLIGHT_CHECK_INTEGRATIONS:azure_devops}") String skipPreflightCheckCommaList,
                                        @Value("${ALLOW_UNSPLIT_ADO:true}") Boolean allowUnsplitAdo,
                                        @Value("${INGESTION_CALLBACK_DISABLED_APPS:jira,harnessng}") String ingestionCallbackDisabledApps,
                                        @Value("${INGESTION_DISABLED_TENANTS:}") String ingestionDisabledTenants) {
        this.inventoryService = inventoryService;
        this.githubTokenService = githubTokenService;
        this.gitRepositoryService = gitRepositoryService;
        this.githubWebhookHandlingServiceUrl = githubWebhookHandlingServiceUrl;
        this.bitbucketTokenService = bitbucketTokenService;
        this.gitlabTokenService = gitlabTokenService;
        this.azureDevopsTokenService = azureDevopsTokenService;
        this.controlPlaneService = controlPlaneService;
        this.inventoryServiceUrl = inventoryUrl;
        this.slackTokenService = slackTokenService;
        this.msTeamsTokenService = msTeamsTokenService;
        this.preflightCheckService = preflightCheckService;
        this.salesForceTokenService = salesForceTokenService;
        this.blackDuckTokenService = blackDuckTokenService;
        this.servicenowTokenService = servicenowTokenService;
        this.slackTenantLookupDatabaseService = slackTenantLookupDatabaseService;
        this.githubAppTokenService = githubAppTokenService;
        this.ingestionTriggerSettings = ingestionTriggerSettings;
        this.cxSastTokenService = cxSastTokenService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.productService = productService;
        this.productIntegMappingService = productIntegMappingService;
        skipPreflightCheckIntegrations = commaListToIntegrationTypeSet(skipPreflightCheckCommaList);
        this.ingestionDisabledTenants = new HashSet<>(CommaListSplitter.split(ingestionDisabledTenants));
        log.info("Skipping preflight check for integrations: {}", skipPreflightCheckIntegrations);
        this.allowUnsplitAdo = BooleanUtils.isTrue(allowUnsplitAdo);
        this.ingestionCallbackDisabledApps = commaListToIntegrationTypeSet(ingestionCallbackDisabledApps);
        this.atlassianConnectServiceClient = atlassianConnectServiceClient;
    }

    @VisibleForTesting
    public static Set<IntegrationType> commaListToIntegrationTypeSet(String list) {
        return CommaListSplitter.splitToStream(list)
                .map(IntegrationType::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void upsertSlackTenantLookUp(IntegrationType integrationType, String teamId, String company, String integrationId) {
        if ((integrationType == IntegrationType.SLACK) && (StringUtils.isNotBlank(teamId))) {
            log.info("Integration type is slack and teamId is present, trying to persist slack tenant lookup");
            try {
                slackTenantLookupDatabaseService.upsert(SlackTenantLookup.builder()
                        .teamId(teamId).tenantName(company)
                        .build());
                log.info("Successfully persisted slack tenant lookup, teamId {}, company {}, integrationId {}", teamId, company, integrationId);
            } catch (SQLException e) {
                log.error("Error persisting slack tenant lookup, teamId {}, company {}, integrationId {}", teamId, company, integrationId, e);
            }
        } else {
            log.error("Integration type {} is NOT slack or teamId is not present {}", integrationType, teamId);
        }
    }

    private void upsertInstanceForIntegration(IntegrationType integrationType, String company, String integrationId, String name, CICD_TYPE cicd_type) {
        if (StringUtils.isNotBlank(integrationId)) {
            try {
                ciCdInstancesDatabaseService.insert(company, CICDInstance.builder()
                        .id(UUID.randomUUID())
                        .name(name + integrationId)
                        .integrationId(integrationId)
                        .type(cicd_type.toString()).build()
                );
            } catch (SQLException e) {
                log.error("Error persisting " + integrationType + " instance lookup, company {}, integrationId {}", company, integrationId, e);
            }
            log.info("Successfully persisted " + integrationType + " instance lookup, company {}, integrationId {}", company, integrationId);
        }
    }

    private void createGithubWebhookSecret(IntegrationType integrationType, String company, String integrationId,
                                           Integration integration) throws InventoryException {
        if ((integrationType == IntegrationType.GITHUB) && StringUtils.isNotBlank(integrationId)) {
            try {
                String secret = generateGithubSecret(company, integrationId);
                Integration.IntegrationBuilder integrationBuilder = getMetadataWithSecrets(integration, Map.of(WEBHOOK_SECRET, secret));
                inventoryService.updateIntegration(company, integrationId, integrationBuilder.build());
            } catch (InventoryException e) {
                log.error("createGithubWebhookSecret: Error creating github secret for company {}, integrationId {}", company, integrationId, e);
                throw e;
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                log.error("createGithubWebhookSecret: Error creating github secret for company {}, integrationId {}", company, integrationId, e);
                throw new InventoryException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Integration.IntegrationBuilder getMetadataWithSecrets(Integration integration, Map<String, String> secrets) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.putAll(MapUtils.emptyIfNull(integration.getMetadata()));
        if (!MapUtils.isEmpty(integration.getMetadata()) && integration.getMetadata().containsKey(SECRETS)) {
            Map<String, String> sec = new HashMap<>();
            sec.putAll((Map<String, String>) integration.getMetadata().get(SECRETS));
            sec.putAll(secrets);
        } else {
            metadata.put(SECRETS, secrets);
        }
        return integration.toBuilder().metadata(metadata);
    }

    @SuppressWarnings("unchecked")
    private Integration.IntegrationBuilder updateIntegrationWithExistingSecrets(String company, String integrationId,
                                                                                Integration.IntegrationBuilder integrationBuilder) throws InventoryException {
        Integration integration = inventoryService.getIntegration(company, integrationId);
        Map<String, Object> metadata = new HashMap<>();
        if (!MapUtils.isEmpty(integration.getMetadata())) {
            metadata.putAll(integration.getMetadata());
        }
        Map<String, String> secrets = new HashMap<>();
        if (metadata.containsKey(SECRETS)) {
            secrets = (Map<String, String>) metadata.get(SECRETS);
        }
        return getMetadataWithSecrets(integrationBuilder.build(), secrets);
    }

    public Integration updateIntegrationFromRequest(String company, String integrationId,
                                                    ModifyIntegrationRequest modifyIntegrationRequest)
            throws InventoryException, NotFoundException, AtlassianConnectServiceClientException {
        if (StringUtils.isBlank(modifyIntegrationRequest.getName())) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "The integration name cannot be empty.");
        }
        Integration.IntegrationBuilder integrationBuilder = Integration.builder()
                .description(modifyIntegrationRequest.getDescription())
                .name(modifyIntegrationRequest.getName().trim())
                .tags(modifyIntegrationRequest.getTags())
                .metadata(modifyIntegrationRequest.getMetadata())
                .id(integrationId);

        IntegrationType integrationType = IntegrationType.fromString(modifyIntegrationRequest.getApplication());

        String teamId = null;
        Token.TokenBuilder tokenBuilder = null;
        //if new 'code' or apikey is provided then we will do an update tokendata flow
        if (StringUtils.isNotEmpty(modifyIntegrationRequest.getCode())
                || StringUtils.isNotEmpty(modifyIntegrationRequest.getApikey())) {

            if (modifyIntegrationRequest.getApplication() == null ||
                    IntegrationType.fromString(modifyIntegrationRequest.getApplication()) == null) {
                throw new ServerApiException(HttpStatus.BAD_REQUEST,
                        "Invalid application provided for integration Request");
            }


            if (!SUPPORTED_APP_TYPES.contains(integrationType)) {
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported application provided.");
            }

            //if things fail, this will throw an exception stopping the updates.
            TokenMetadata tokenMetadata = getUrlAndTokenBuilder(company, modifyIntegrationRequest);
            tokenBuilder = tokenMetadata.getTokenBuilder();
            teamId = (integrationType == IntegrationType.SLACK) ? tokenMetadata.getTeamId() : null;

            integrationBuilder.application(modifyIntegrationRequest.getApplication())
                    .status("ACTIVE")
                    .metadata(modifyIntegrationRequest.getMetadata())
                    .url(modifyIntegrationRequest.getUrl());
        }

        updateIntegrationWithExistingSecrets(company, integrationId, integrationBuilder);
        log.info("Updating integration={} for company={} with: {}", integrationId, company, integrationBuilder.build());
        inventoryService.updateIntegration(company, integrationId,
                        integrationBuilder.build())
                .orElseThrow(() -> new NotFoundException("Could not update integration with id=" + integrationId));

        //Upsert Slack Tenant Lookup
        upsertSlackTenantLookUp(integrationType, teamId, company, integrationId);

        //update the tokens
        if (tokenBuilder != null) {
            inventoryService.deleteTokensByIntegration(company, integrationId);

            log.info("Persisting token data for integration={} company={}", integrationId, company);
            tokenBuilder.integrationId(integrationId);
            inventoryService.postToken(company, integrationId, tokenBuilder.build());
        }

        if (BooleanUtils.isTrue(modifyIntegrationRequest.getStartIngestion())) {
            try {
                Integration dbIntegration = inventoryService.getIntegration(company, integrationId);
                resumeIngestion(company, integrationId, dbIntegration.getApplication());
            } catch (IngestionServiceException e) {
                log.warn("Failed to resume ingestion for company={} integration={}", company, integrationId, e);
            }
        }

        return integrationBuilder.build();
    }

    public Integration createIntegrationFromRequest(String company, ModifyIntegrationRequest modifyIntegrationRequest)
            throws InventoryException, PreflightCheckFailedException, BadRequestException, AtlassianConnectServiceClientException {

        log.info("Creating Integration for company={} integration_type={} name='{}' using {}", company,
                modifyIntegrationRequest.getApplication(), modifyIntegrationRequest.getName(), modifyIntegrationRequest.getType());

        IntegrationType integrationType = IntegrationType.fromString(modifyIntegrationRequest.getApplication());
        if (modifyIntegrationRequest.getApplication() == null || integrationType == null) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "Invalid application provided for integration Request");
        }
        if (StringUtils.isBlank(modifyIntegrationRequest.getName())) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "The integration name can't be empty");
        }
        if (!SUPPORTED_APP_TYPES.contains(integrationType)) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported application provided.");
        }

        boolean satellite = BooleanUtils.isTrue(modifyIntegrationRequest.getSatellite());
        Integration integration = Integration.builder()
                .application(modifyIntegrationRequest.getApplication())
                .description(modifyIntegrationRequest.getDescription())
                .name(modifyIntegrationRequest.getName().trim())
                .status("ACTIVE")
                .tags(modifyIntegrationRequest.getTags())
                .url(modifyIntegrationRequest.getUrl())
                .satellite(satellite)
                .metadata(MapUtils.emptyIfNull(modifyIntegrationRequest.getMetadata()))
                .authentication(Integration.Authentication.NONE) // none means no auth secrets stored in SM (this will be overwritten if a token ends up being generated)
                .build();

        // PROP-1233 For ADO, we will split the request and create up to 3 integrations (one per subtype)
        if (integrationType == IntegrationType.AZURE_DEVOPS) {
            return splitAndCreateAdoIntegrations(company, modifyIntegrationRequest, integration, integrationType, satellite);
        }

        return doCreateIntegrationFromRequest(company, modifyIntegrationRequest, integration, integrationType, satellite);
    }

    private Integration splitAndCreateAdoIntegrations(String company, ModifyIntegrationRequest modifyIntegrationRequest, Integration integration, IntegrationType integrationType, boolean satellite) throws BadRequestException, InventoryException, PreflightCheckFailedException, AtlassianConnectServiceClientException {
        List<Integration> integrations = splitAdoIntegration(integration);
        if (integrations.isEmpty()) {
            if (allowUnsplitAdo) {
                log.info("Request to install ADO did not specify subtypes, keeping it unsplit for company={}", company);
                return doCreateIntegrationFromRequest(company, modifyIntegrationRequest, integration, integrationType, satellite);
            }
            throw new BadRequestException("Invalid metadata provided: at least one sub type must be specified");
        }
        log.info("Split ADO integration request into {} sub types for {}", integrations.size(), company);

        // check for name collisions
        Set<String> integrationNames = integrations.stream().map(Integration::getName).map(String::toLowerCase).collect(Collectors.toSet());
        Optional<String> nameCollision = inventoryService.listIntegrationsByApp(company, IntegrationType.AZURE_DEVOPS.toString()).getRecords().stream()
                .map(Integration::getName)
                .map(String::toLowerCase)
                .filter(integrationNames::contains)
                .findAny();
        if (nameCollision.isPresent()) {
            throw new BadRequestException("Integration name already in use: " + nameCollision.get());
        }

        List<Integration> integrationsWithIds = new ArrayList<>();

        // create "parent" integration, that will hold the credentials
        Integration parentIntegration = doCreateIntegrationFromRequest(company, modifyIntegrationRequest, integrations.get(0), integrationType, satellite);
        integrationsWithIds.add(parentIntegration);

        // create "child" integrations, that will be linked to the parent's credentials
        for (int i = 1; i < integrations.size(); ++i) {
            Integration childIntegration = integrations.get(i).toBuilder()
                    .linkedCredentials(parentIntegration.getId())
                    .build();
            integrationsWithIds.add(
                    doCreateIntegrationFromRequest(company, modifyIntegrationRequest, childIntegration, integrationType, satellite));
        }

        // collect all ids and return them with the parent integration
        Map<String, String> idsBySubType = integrationsWithIds.stream().collect(Collectors.toMap(
                i -> (String) i.getMetadata().getOrDefault(ADO_SUBTYPE_METADATA_FIELD, "unknown"),
                i -> String.valueOf(i.getId()),
                (a, b) -> b
        ));

        log.info("Created ADO integrations for {}: {}", company, idsBySubType);
        return parentIntegration.toBuilder()
                .metadata(MapUtils.append(parentIntegration.getMetadata(), "integration_ids", idsBySubType))
                .build();
    }

    private Integration doCreateIntegrationFromRequest(String company, ModifyIntegrationRequest modifyIntegrationRequest, Integration integration, IntegrationType integrationType, boolean satellite) throws PreflightCheckFailedException, InventoryException, AtlassianConnectServiceClientException {
        Token token = null;
        String teamId = null;
        if (!satellite && StringUtils.isEmpty(integration.getLinkedCredentials())) {
            TokenMetadata tokenMetadata = getUrlAndTokenBuilder(company, modifyIntegrationRequest);
            token = tokenMetadata.getTokenBuilder().build();
            teamId = (integrationType == IntegrationType.SLACK) ? tokenMetadata.getTeamId() : null;

            // persist the authentication type
            integration = integration.toBuilder()
                    .authentication(getAuthenticationMethod(modifyIntegrationRequest))
                    .build();

            //for salesforce, the url needs to be overridden with response instanceUrl.
            if (integrationType == IntegrationType.SALESFORCE) {
                integration = integration.toBuilder()
                        .url(tokenMetadata.getUrl())
                        .build();
            }

            if (StringUtils.isNotBlank(modifyIntegrationRequest.getGhaInstallationId())) {
                // If this is a Harness hosted GHA integration, we need to add the installation_id and app_id to the metadata
                Map<String, Object> metadata = new HashMap<>(integration.getMetadata());
                metadata.put("installation_id", modifyIntegrationRequest.getGhaInstallationId());
                metadata.put("app_id", githubAppTokenService.getSeiAppId());
                metadata.put("harness_gha", true);
                integration = integration.toBuilder()
                        .metadata(metadata)
                        .build();
            }

            // preflight check
            if (BooleanUtils.isTrue(modifyIntegrationRequest.getSkipPreflightCheck()) || skipPreflightCheckIntegrations.contains(integrationType)) {
                log.info("Skipping preflight check for company={} integration_type={}", company, integration.getApplication());
            } else {
                log.info("Running preflight check for company={} integration_type={}", company, integration.getApplication());
                PreflightCheckResults preflightCheckResults = preflightCheckService.check(company, integration, token);
                if (!preflightCheckResults.isSuccess()) {
                    throw new PreflightCheckFailedException(preflightCheckResults);
                }
            }
        }

        log.info("Persisting integration and token (if relevant) for company={} integration_type={}", company, integration.getApplication());

        // persist integration
        String integrationId = inventoryService.postIntegration(company, integration)
                .orElseThrow(() -> new ServerApiException("Failed to create integration."));
        integration = integration.toBuilder()
                .id(integrationId)
                .build();

        // add it to a product
        addIntegrationToDefaultProduct(company, integrationId);

        try {
            //Upsert Slack Tenant Lookup
            upsertSlackTenantLookUp(integrationType, teamId, company, integrationId);

            String subtype = (String) MapUtils.emptyIfNull(integration.getMetadata()).get("subtype");
            if (integrationType == IntegrationType.AZURE_DEVOPS && (subtype == null || AdoSubtype.CICD.getMetadataField().equals(subtype))) {
                // we only want to create the cicd instance for ADO/CICD subtype (or when subtype is not specified for backward compatibility with unsplit ADO)
                upsertInstanceForIntegration(integrationType, company, integrationId, "azure-integration-", CICD_TYPE.azure_devops);
            }
            if (integrationType == IntegrationType.GITLAB) {
                upsertInstanceForIntegration(integrationType, company, integrationId, "gitlab-integration-", CICD_TYPE.gitlab);
            }
            if (integrationType == IntegrationType.CIRCLECI) {
                upsertInstanceForIntegration(integrationType, company, integrationId, "circleci-integration-", CICD_TYPE.circleci);
            }
            if (integrationType == IntegrationType.DRONECI) {
                upsertInstanceForIntegration(integrationType, company, integrationId, "droneci-integration-", CICD_TYPE.droneci);
            }
            if (integrationType == IntegrationType.HARNESSNG) {
                upsertInstanceForIntegration(integrationType, company, integrationId, "harnessng-integration-", CICD_TYPE.harnessng);
            }
            if (integrationType == IntegrationType.GITHUB_ACTIONS) {
                upsertInstanceForIntegration(integrationType, company, integrationId, "github-actions-integration-", CICD_TYPE.github_actions);
            }
            if (integrationType == IntegrationType.GITHUB) {
                createGithubWebhookSecret(integrationType, company, integrationId, integration);
                Integration dbIntegration = inventoryService.getIntegration(company, integrationId);
                String webhookSecret = getWebhookSecret(dbIntegration);
                if (StringUtils.isNotEmpty(webhookSecret) && isPushBasedIntegration(dbIntegration) && isAutoRegisterWebhook(dbIntegration)) {
                    try {
                        dispatchCreateWebhookJobRequest(company, integrationId, webhookSecret, dbIntegration.getSatellite());
                    } catch (IngestionServiceException e) {
                        log.error("Could not create webhooks for company " + company + " and integrationId "
                                + integrationId, e);
                    }
                }
            }
            if (token != null) {
                // persist token
                token = token.toBuilder()
                        .integrationId(integrationId)
                        .build();
                inventoryService.postToken(company, integrationId, token);
            }

            // create trigger for ingestion
            try {
                boolean startIngestion = BooleanUtils.isNotFalse(modifyIntegrationRequest.getStartIngestion());
                setupIngestionTrigger(company, integration, modifyIntegrationRequest.getApplication(), satellite, startIngestion);
            } catch (IngestionServiceException e) {
                log.error("Failed to create Ingestion Jobs");
                throw new ServerApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create Ingestion jobs.");
            }

            return integration;

        } catch (Exception e) {
            // if any error occurs after we have persisted the integration,
            // we should clean it up before propagating
            try {
                inventoryService.deleteIntegration(company, integrationId);
            } catch (Exception e1) {
                log.error("Could not clean-up failed integration for company={}, integration_id={}", company, integrationId, e);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    protected static List<Integration> splitAdoIntegration(Integration integration) {
        Map<String, Object> metadata = new HashMap<>(MapUtils.emptyIfNull(integration.getMetadata()));

        Function<AdoSubtype, Map<String, Object>> extractor = subtype -> {
            var extractedMetadata = (Map<String, Object>) metadata.getOrDefault(subtype.getMetadataField(), Map.of());
            metadata.remove(subtype.getMetadataField());
            return extractedMetadata;
        };
        Map<String, Object> wi = extractor.apply(AdoSubtype.WI);
        Map<String, Object> scm = extractor.apply(AdoSubtype.SCM);
        Map<String, Object> cicd = extractor.apply(AdoSubtype.CICD);

        List<Integration> integrations = new ArrayList<>(3);
        BiConsumer<AdoSubtype, Map<String, Object>> merger = (subtype, subtypeMetadata) -> {
            boolean enabled = BooleanUtils.isTrue((Boolean) subtypeMetadata.getOrDefault(ADO_ENABLED_METADATA_FIELD, false));
            if (!enabled) {
                return;
            }
            Map<String, Object> merged = MapUtils.merge(metadata, subtypeMetadata);
            merged.remove(ADO_ENABLED_METADATA_FIELD); // not needed anymore
            merged.put(ADO_SUBTYPE_METADATA_FIELD, subtype.getMetadataField());
            subtype.getIngestionFlagsComplement().forEach(flag -> merged.put(flag, false)); // disable flags of other subtypes
            subtype.getIngestionFlags().forEach(flag -> merged.putIfAbsent(flag, true)); // enable flags of current subtype (unless already specified)
            integrations.add(integration.toBuilder()
                    .name(subtype.generateIntegrationName(integration.getName()))
                    .metadata(merged)
                    .build());
        };
        merger.accept(AdoSubtype.WI, wi);
        merger.accept(AdoSubtype.SCM, scm);
        merger.accept(AdoSubtype.CICD, cicd);


        return integrations;
    }

    public Integration getIntegration(String company, String integrationId) throws InventoryException {
        return removeSecrets(inventoryService.getIntegration(company, integrationId));
    }

    public DbListResponse<Integration> listIntegrations(String company, DefaultListRequest filter) throws InventoryException {
        DbListResponse<Integration> integrationDbListResponse = inventoryService.listIntegrationsFullFilter(company, filter);
        List<Integration> collect = integrationDbListResponse
                .getRecords()
                .stream()
                .map(this::removeSecrets)
                .collect(Collectors.toList());
        Integer totalCount = integrationDbListResponse.getTotalCount();
        return DbListResponse.of(collect, totalCount);
    }

    private Integration removeSecrets(Integration integration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.putAll(MapUtils.emptyIfNull(integration.getMetadata()));
        metadata.remove(SECRETS);
        return integration.toBuilder().metadata(metadata).build();
    }

    private void addIntegrationToDefaultProduct(String company, String integrationId) {
        Optional<String> defaultProductId;
        try {
            defaultProductId = productService.getSystemImmutableProducts(company).stream()
                    .filter(product -> DEFAULT_PRODUCT_KEY.equalsIgnoreCase(product.getKey()))
                    .map(Product::getId)
                    .findFirst();
        } catch (SQLException e) {
            log.warn("Failed to get default product for tenant={}", company, e);
            return;
        }
        if (defaultProductId.isEmpty()) {
            log.warn("No default product configured for tenant={}!", company);
            return;
        }

        try {
            productIntegMappingService.insert(company, ProductIntegMapping.builder()
                    .integrationId(integrationId)
                    .productId(defaultProductId.get())
                    .mappings(Map.of())
                    .build());
        } catch (SQLException e) {
            log.warn("Failed to add integration={} to product={} for tenant={}", integrationId, defaultProductId.get(), company, e);
        }
    }

    @VisibleForTesting
    public static String getCallbackUrl(
            IntegrationType integrationType,
            String company,
            String integrationId,
            String inventoryServiceUrl,
            Set<IntegrationType> ingestionCallbackDisabledApps) {
        String callbackUrl = "";
        // Temporary code until we migrate all apps from aggs to ETL framework
        if (!ingestionCallbackDisabledApps.contains(integrationType)) {
            callbackUrl = inventoryServiceUrl + "/internal/v1/tenants/" + company + "/integrations/"
                    + integrationId + "/push";
        }
        return callbackUrl;
    }

    private void setupIngestionTrigger(String company, Integration integration, String application, boolean satellite, boolean startIngestion)
            throws IngestionServiceException {
        // we don't have anything queue for slack
        IntegrationType integrationType = IntegrationType.fromString(application);

        if (SUPPRESSED_TRIGGER_INTEGRATIONS.contains(integrationType)) {
            log.info("Integration Type " + integrationType + " Trigger creation is suppressed!");
            return;
        }
        String callbackUrl = getCallbackUrl(integrationType, company, integration.getId(), inventoryServiceUrl, ingestionCallbackDisabledApps);


        int triggerFrequency = startIngestion ? ingestionTriggerSettings.getTriggerFrequency(application) : 0;
        if (ingestionDisabledTenants.contains(company)) {
            log.info("Tenant {} found in ingestion disabled tenants set, setting ingestion frequency to 0", company);
            triggerFrequency = 0;
        }
        log.info("Setting up ingestion trigger for tenant={}, integrationId={}, application={}," +
                " satellite={}, frequency={}min", company, integration.getId(), application, satellite, triggerFrequency);
        controlPlaneService.createTrigger(
                CreateTriggerRequest.builder()
                        .integrationKey(
                                IntegrationKey.builder()
                                        .integrationId(integration.getId())
                                        .tenantId(company)
                                        .build())
                        .callbackUrl(callbackUrl)
                        .frequency(triggerFrequency) //minutes
                        .triggerType(application)
                        .reserved(satellite) // "reserve" triggered jobs so that only satellites can pull them
                        .build());
    }

    private void resumeIngestion(String company, String integrationId, String application) throws IngestionServiceException {
        // so far, each integration only has 1 trigger
        DbTrigger trigger = IterableUtils.getFirst(controlPlaneService.getTriggers(company, 0, integrationId).getResponse().getRecords())
                .orElse(null);

        // if there is no trigger, ignore
        if (trigger == null) {
            return;
        }
        // if trigger is already running, ignore
        if (trigger.getFrequency() != null && trigger.getFrequency() > 0) {
            return;
        }

        int frequency = ingestionTriggerSettings.getTriggerFrequency(application);
        log.info("Starting ingestion for company={} integration={} freq={}", company, integrationId, frequency);
        controlPlaneService.updateTriggerFrequency(trigger.getId(), frequency);
    }

    public void dropIngestionTrigger(String company, String integrationId)
            throws IngestionServiceException {
        controlPlaneService.deleleTriggersByIntegrationKey(
                IntegrationKey.builder()
                        .integrationId(integrationId)
                        .tenantId(company)
                        .build());
    }

    public boolean isIntegrationCountMaxedOut(String company, ModifyIntegrationRequest request, Map<String, String> entitlementsConfig) throws InventoryException {

        List<Integration> integrationList = inventoryService.listIntegrations(company).getRecords();

        if (IntegrationType.fromString(request.getApplication()).isScmFamily() && entitlementsConfig.containsKey("SETTING_SCM_INTEGRATIONS_COUNT")) {
            long maxCount = Long.valueOf(entitlementsConfig.get("SETTING_SCM_INTEGRATIONS_COUNT"));
            long currentScmIntegrationCount = integrationList.stream().filter(integration -> IntegrationType.fromString(request.getApplication()).isScmFamily())
                    .count();
            return currentScmIntegrationCount >= maxCount;
        }

        return false;
    }

    @lombok.Value
    @Builder
    public static class TokenMetadata {
        String url;
        Token.TokenBuilder tokenBuilder;
        String teamId;
    }

    public static Integration.Authentication getAuthenticationMethod(ModifyIntegrationRequest modifyIntegrationRequest) {
        if (modifyIntegrationRequest == null || modifyIntegrationRequest.getType() == null) {
            return Integration.Authentication.NONE;
        }
        switch (modifyIntegrationRequest.getType()) {
            case ApiKey.TOKEN_TYPE:
                return Integration.Authentication.API_KEY;
            case MultipleApiKeys.TOKEN_TYPE:
                return Integration.Authentication.MULTIPLE_API_KEYS;
            case OauthToken.TOKEN_TYPE:
                return Integration.Authentication.OAUTH;
            case DBAuth.TOKEN_TYPE:
                return Integration.Authentication.DB;
            case AtlassianConnectJwtToken.TOKEN_TYPE:
                return Integration.Authentication.ATLASSIAN_CONNECT_JWT;
            default:
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported token_type provided.");
        }
    }

    private TokenMetadata getUrlAndTokenBuilder(
            String company,
            ModifyIntegrationRequest modifyIntegrationRequest)
            throws ServerApiException, AtlassianConnectServiceClientException {
        if (modifyIntegrationRequest.getType() == null) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "Invalid type provided for integration Request");
        }
        Long createdTime = System.currentTimeMillis() / 1000;
        switch (modifyIntegrationRequest.getType()) {
            case ApiKey.TOKEN_TYPE:
                return TokenMetadata.builder()
                        .tokenBuilder(Token.builder().tokenData(ApiKey.builder()
                                .apiKey(modifyIntegrationRequest.getApikey())
                                .userName(modifyIntegrationRequest.getUsername())
                                .createdAt(createdTime)
                                .build()))
                        .build();
            case MultipleApiKeys.TOKEN_TYPE:
                List<MultipleApiKeys.Key> keys = CollectionUtils.emptyIfNull(modifyIntegrationRequest.getKeys()).stream().map(k -> MultipleApiKeys.Key.builder().apiKey(k.getApikey()).userName(k.getUsername()).build()).collect(Collectors.toList());
                return TokenMetadata.builder()
                        .tokenBuilder(Token.builder().tokenData(MultipleApiKeys.builder()
                                .keys(keys)
                                .createdAt(createdTime)
                                .build()))
                        .build();
            case OauthToken.TOKEN_TYPE:
                log.info("attempting to get oauth token for company={} app={}", company, modifyIntegrationRequest.getApplication());
                TokenService.Tokens tokens = getTokens(company, modifyIntegrationRequest);
                log.info("tokens = {}", tokens);
                return TokenMetadata.builder()
                        .url(tokens.getInstanceUrl())
                        .tokenBuilder(Token.builder().tokenData(OauthToken.builder()
                                .token(tokens.getAccessToken())
                                .refreshToken(tokens.getRefreshToken())
                                .botToken(tokens.getBotToken())
                                .instanceUrl(tokens.getInstanceUrl())
                                .createdAt(createdTime)
                                .build()))
                        .teamId(tokens.getTeamId())
                        .build();
            case DBAuth.TOKEN_TYPE:
                return TokenMetadata.builder()
                        .tokenBuilder(Token.builder().tokenData(DBAuth.builder()
                                .name(modifyIntegrationRequest.getName())
                                .server(modifyIntegrationRequest.getServer())
                                .userName(modifyIntegrationRequest.getUsername())
                                .password(modifyIntegrationRequest.getPassword())
                                .databaseName(modifyIntegrationRequest.getDatabaseName())
                                .build()))
                        .build();
            case AtlassianConnectJwtToken.TOKEN_TYPE:
                AtlassianConnectAppMetadata metadata =
                        atlassianConnectServiceClient.getMetadata(modifyIntegrationRequest.getClientKey());
                String secret = atlassianConnectServiceClient.getSecret(modifyIntegrationRequest.getClientKey());
                return TokenMetadata.builder()
                        .tokenBuilder(Token.builder()
                                .tokenData(AtlassianConnectJwtToken.builder()
                                        .name(modifyIntegrationRequest.getName())
                                        .appKey(metadata.getInstalledAppKey())
                                        .sharedSecret(secret)
                                        .clientKey(modifyIntegrationRequest.getClientKey())
                                        .baseUrl(metadata.getAtlassianBaseUrl())
                                        .build()))
                        .url(metadata.getAtlassianBaseUrl())
                        .build();
            default:
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported token_type provided.");
        }
    }

    private TokenService.Tokens getTokens(String company, ModifyIntegrationRequest modifyIntegrationRequest)
            throws ServerApiException {

        IntegrationType integrationType = IntegrationType.fromString(modifyIntegrationRequest.getApplication());
        if (integrationType == null) {
            throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported application provided: " + modifyIntegrationRequest.getApplication());
        }

        TokenService tokenService = null;
        try {
            switch (integrationType) {
                case GITHUB_ACTIONS:
                case GITHUB:
                    // GitHub Apps use private keys and JWT tokens, compared to traditional OAuth tokens
                    if (StringUtils.isNotBlank(modifyIntegrationRequest.getPrivateKey())) {
                        return generatePrivateGithubAppToken(modifyIntegrationRequest);
                    } else if (StringUtils.isNotBlank(modifyIntegrationRequest.getGhaInstallationId())) {
                        return generatePublicGithubAppToken(modifyIntegrationRequest);
                    } else {
                        tokenService = githubTokenService;
                    }
                    break;
                case BITBUCKET:
                    tokenService = bitbucketTokenService;
                    break;
                case SLACK:
                    tokenService = slackTokenService;
                    break;
                case MS_TEAMS:
                    tokenService = msTeamsTokenService;
                    break;
                case SALESFORCE:
                    tokenService = salesForceTokenService;
                    break;
                case GITLAB:
                    tokenService = gitlabTokenService;
                    break;
                case AZURE_DEVOPS:
                    tokenService = azureDevopsTokenService;
                    break;
                case CXSAST:
                    String username = modifyIntegrationRequest.getUsername();
                    String password = modifyIntegrationRequest.getPassword();
                    String refreshToken = username + ":" + password;
                    return cxSastTokenService.generateToken(refreshToken, modifyIntegrationRequest.getUrl());
                case BLACKDUCK:
                    return blackDuckTokenService.generateToken(modifyIntegrationRequest.getApikey(), modifyIntegrationRequest.getUrl());
                case SERVICENOW:
                    return servicenowTokenService.getTokens(modifyIntegrationRequest.getUsername(), modifyIntegrationRequest.getPassword(), modifyIntegrationRequest.getUrl());
                default:
                    throw new ServerApiException(HttpStatus.BAD_REQUEST, "Unsupported application provided: " + modifyIntegrationRequest.getApplication());
            }
            return tokenService.getTokensFromCode(modifyIntegrationRequest.getCode(), modifyIntegrationRequest.getState());
        } catch (TokenException e) {
            log.error("Failed to generate access token for company={}, app={}", company, modifyIntegrationRequest.getApplication(), e);
            throw new ServerApiException(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private TokenService.Tokens generatePrivateGithubAppToken(ModifyIntegrationRequest modifyIntegrationRequest) throws TokenException {
        Map<String, Object> metadata = MapUtils.emptyIfNull(modifyIntegrationRequest.getMetadata());
        String appId = (String) metadata.get("app_id");
        String installationId = (String) metadata.get("installation_id");
        return generateGithubAppToken(modifyIntegrationRequest.getUrl(), appId, installationId, modifyIntegrationRequest.getPrivateKey());
    }

    private TokenService.Tokens generatePublicGithubAppToken(ModifyIntegrationRequest modifyIntegrationRequest) throws TokenException {
        String installationID = modifyIntegrationRequest.getGhaInstallationId();
        try {
            return githubAppTokenService.getTokenForSeiApp(modifyIntegrationRequest.getUrl(), installationID);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new TokenException("Failed to generate access token from GitHub App JWT", e);
        }
    }

    private TokenService.Tokens generateGithubAppToken(String url, String appId, String installationId, String privateKey) throws TokenException {
        try {
            String accessToken = githubAppTokenService.generateAccessToken(url, appId, installationId, privateKey);
            return TokenService.Tokens.builder()
                    .accessToken(accessToken)
                    .refreshToken(privateKey)
                    .build();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new TokenException("Failed to generate access token from GitHub App JWT", e);
        }
    }

    private String generateGithubSecret(String company, String id) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKey = new SecretKeySpec(company.getBytes(), "HmacSHA256");
        Mac githubSha256 = Mac.getInstance("HmacSHA256");
        githubSha256.init(secretKey);
        return Base64.encodeBase64String(githubSha256.doFinal(id.getBytes()));
    }

    private void dispatchCreateWebhookJobRequest(String customer, String integrationId,
                                                 String webhookSecret, Boolean isSatellite) throws IngestionServiceException {
        controlPlaneService.submitJob(CreateJobRequest.builder()
                .controllerName(WEBHOOK_CONTROLLER)
                .integrationId(integrationId)
                .tenantId(customer)
                .reserved(isSatellite)
                .query(GithubCreateWebhookQuery.builder()
                        .integrationKey(IntegrationKey.builder()
                                .tenantId(customer)
                                .integrationId(integrationId)
                                .build())
                        .organizations(List.of())
                        .secret(webhookSecret)
                        .url(createWebhookUrl(customer, integrationId))
                        .events(GITHUB_WEBHOOK_EVENTS)
                        .build())
                .build());
    }

    private String getWebhookSecret(Integration integration) {
        Map<String, Object> metadata = integration.getMetadata();
        String webhookSecret = null;
        if (org.apache.commons.collections4.MapUtils.isNotEmpty(metadata) && metadata.containsKey(SECRETS)) {
            Map<String, String> secret = (Map<String, String>) metadata.get(SECRETS);
            if (org.apache.commons.collections4.MapUtils.isNotEmpty(secret) && secret.containsKey(WEBHOOK_SECRET)) {
                webhookSecret = secret.get(WEBHOOK_SECRET);
            }
        }
        return webhookSecret;
    }

    private boolean isPushBasedIntegration(Integration integration) {
        Map<String, Object> metadata = integration.getMetadata();
        return (org.apache.commons.collections4.MapUtils.isNotEmpty(metadata) && metadata.containsKey(IS_PUSH_BASED) &&
                (boolean) metadata.get(IS_PUSH_BASED));
    }

    private boolean isAutoRegisterWebhook(Integration integration) {
        if (!MapUtils.isEmpty(integration.getMetadata()) && integration.getMetadata().containsKey(AUTO_REGISTER_WEBHOOK)) {
            return Boolean.TRUE.equals(integration.getMetadata().get(AUTO_REGISTER_WEBHOOK));
        }
        return true;
    }

    private String createWebhookUrl(String customer, String integrationId) {
        return githubWebhookHandlingServiceUrl + "/v1/webhooks/github/notifications/" + customer + "/" + integrationId;
    }

}
