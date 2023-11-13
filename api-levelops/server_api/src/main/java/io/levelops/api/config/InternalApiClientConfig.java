package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.plugins.clients.IntegrationAggregationsClient;
import io.levelops.aggregations.plugins.clients.PluginResultAggregationsClient;
import io.levelops.automation_rules.AutomationRuleHitsClient;
import io.levelops.automation_rules.AutomationRulesClient;
import io.levelops.commons.generic.clients.GenericRequestsClient;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.config_tables.clients.ConfigTableClient;
import io.levelops.ingestion.clients.IngestionClient;
import io.levelops.io.levelops.scm_repo_mapping.clients.ScmRepoMappingClient;
import io.levelops.objects.ObjectsClient;
import io.levelops.plugins.clients.PluginResultsClient;
import io.levelops.questionnaires.clients.QuestionnaireClient;
import io.levelops.questionnaires.clients.QuestionnaireTemplateClient;
import io.levelops.triage.clients.StoredFiltersRESTClient;
import io.levelops.triage.clients.TriageRESTClient;
import io.levelops.users.clients.UsersRESTClient;
import io.levelops.workitems.clients.QuestionnaireNotificationClient;
import io.levelops.workitems.clients.WorkItemsClient;
import io.levelops.workitems.clients.WorkItemsNotificationClient;
import io.levelops.workitems.clients.WorkItemsRESTClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalApiClientConfig {

    @Bean
    public InventoryService inventoryService(ObjectMapper objectMapper,
                                             OkHttpClient okHttpClient,
                                             @Qualifier("internalApiUrl") String internalApiUri) {
        return InventoryServiceImpl.builder()
                .inventoryServiceUrl(internalApiUri)
                .objectMapper(objectMapper)
                .client(okHttpClient)
                .build();
    }

    @Bean
    public PluginResultsClient pluginResultsClient(OkHttpClient client,
                                                   ObjectMapper objectMapper,
                                                   @Qualifier("internalApiUrl") String internalApiUri) {
        return new PluginResultsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public IntegrationAggregationsClient integrationAggregationsClient(OkHttpClient client,
                                                                       ObjectMapper objectMapper,
                                                                       @Qualifier("internalApiUrl") String internalApiUri) {
        return new IntegrationAggregationsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public PluginResultAggregationsClient pluginResultAggregationsClient(OkHttpClient client,
                                                                         ObjectMapper objectMapper,
                                                                         @Qualifier("internalApiUrl") String internalApiUri) {
        return new PluginResultAggregationsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public QuestionnaireTemplateClient questionnaireTemplateClient(final OkHttpClient client,
                                                                   final ObjectMapper objectMapper,
                                                                   @Qualifier("internalApiUrl") String internalApiUri) {
        return QuestionnaireTemplateClient.builder()
                .client(client)
                .objectMapper(objectMapper)
                .internalApiUri(internalApiUri)
                .build();
    }

    @Bean
    public QuestionnaireClient questionnaireClient(final OkHttpClient client,
                                                   final ObjectMapper objectMapper,
                                                   @Qualifier("internalApiUrl") String internalApiUri,
                                                   @Value("${EXPORT_SERVICE_URL:http://export-service}") String exportServiceUri) {
        return QuestionnaireClient.builder()
                .client(client)
                .objectMapper(objectMapper)
                .internalApiUri(internalApiUri)
                .exportServiceUri(exportServiceUri)
                .build();
    }

    @Bean
    public TriageRESTClient triageRESTClient(final OkHttpClient client,
                                             final ObjectMapper objectMapper,
                                             @Qualifier("internalApiUrl") String internalApiUri) {
        return new TriageRESTClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public StoredFiltersRESTClient storedFiltersRESTClient(final OkHttpClient client,
                                                            final ObjectMapper objectMapper,
                                                            @Qualifier("internalApiUrl") String internalApiUri) {
        return new StoredFiltersRESTClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public AutomationRulesClient automationRulesClient(final OkHttpClient client,
                                                  final ObjectMapper objectMapper,
                                                  @Qualifier("internalApiUrl") String internalApiUri) {
        return new AutomationRulesClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public AutomationRuleHitsClient automationRuleHitsClient(final OkHttpClient client,
                                                             final ObjectMapper objectMapper,
                                                             @Qualifier("internalApiUrl") String internalApiUri) {
        return new AutomationRuleHitsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public ObjectsClient objectsClient(final OkHttpClient client,
                                       final ObjectMapper objectMapper,
                                       @Qualifier("internalApiUrl") String internalApiUri) {
        return new ObjectsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public UsersRESTClient usersRESTClient(final OkHttpClient client,
                                           final ObjectMapper objectMapper,
                                           @Qualifier("internalApiUrl") String internalApiUri) {
        return new UsersRESTClient(client, objectMapper, internalApiUri);
    }


    @Bean
    public WorkItemsClient workItemsClient(final OkHttpClient client,
                                           final ObjectMapper objectMapper,
                                           @Qualifier("internalApiUrl") String internalApiUri) {
        return WorkItemsRESTClient.builder()
                .client(client)
                .mapper(objectMapper)
                .apiBaseUrl(internalApiUri)
                .build();
    }

    @Bean
    public WorkItemsNotificationClient workItemsNotificationClient(final OkHttpClient client,
                                                                   final ObjectMapper objectMapper,
                                                                   @Qualifier("internalApiUrl") String internalApiUri) {
        return WorkItemsNotificationClient.builder()
                .client(client)
                .mapper(objectMapper)
                .apiBaseUrl(internalApiUri)
                .build();
    }

    @Bean
    public GenericRequestsClient genericRequestsClient(final OkHttpClient client,
                                                       final ObjectMapper objectMapper,
                                                       @Qualifier("internalApiUrl") String internalApiUri) {
        return GenericRequestsClient.builder()
                .client(client)
                .mapper(objectMapper)
                .apiBaseUrl(internalApiUri)
                .build();
    }

    @Bean
    public ConfigTableClient configTableClient(final OkHttpClient client,
                                               final ObjectMapper objectMapper,
                                               @Qualifier("internalApiUrl") String internalApiUri) {
        return new ConfigTableClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public QuestionnaireNotificationClient questionnaireNotificationClient(OkHttpClient client,
                                                               ObjectMapper objectMapper,
                                                               @Qualifier("internalApiUrl") String internalApiUri) {
        return new QuestionnaireNotificationClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public IngestionClient ingestionClient(final OkHttpClient client,
                                           final ObjectMapper objectMapper,
                                           @Qualifier("internalApiUrl") String internalApiUri) {
        return new IngestionClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public ScmRepoMappingClient scmRepoMappingClient(final OkHttpClient client,
                                                     final ObjectMapper objectMapper,
                                                     @Qualifier("internalApiUrl") String internalApiUri) {
        return new ScmRepoMappingClient(client, objectMapper, internalApiUri);
    }


}
