package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.token_services.*;
import io.levelops.integrations.github.client.GithubAppTokenService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenConfig {

    @Bean
    public GithubTokenService.GithubSecrets githubSecrets(@Value("${GITHUB_CLIENT_ID}") String clientId,
                                                          @Value("${GITHUB_CLIENT_SECRET}") String clientSecret) {
        return GithubTokenService.GithubSecrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    public BitbucketTokenService.Secrets bitbucketSecrets(@Value("${BITBUCKET_CLIENT_ID}") String clientId,
                                                          @Value("${BITBUCKET_CLIENT_SECRET}") String clientSecret) {
        return BitbucketTokenService.Secrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    public GitlabTokenService.Secrets gitlabSecrets(@Value("${GITLAB_CLIENT_ID:asdiasd}") String clientId,
                                                    @Value("${GITLAB_CLIENT_SECRET:askndkas}") String clientSecret) {
        return GitlabTokenService.Secrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }


    @Bean
    public BlackDuckTokenService blackDuckTokenService(OkHttpClient okHttpClient, ObjectMapper mapper) {
        return new BlackDuckTokenService(okHttpClient, mapper);
    }

    @Bean
    public AzureDevopsTokenService.AzureDevopsSecrets azureDevopsSecrets(@Value("${AZURE_DEVOPS_CLIENT_ID}") String clientId,
                                                                         @Value("${AZURE_DEVOPS_CLIENT_SECRET}") String clientSecret) {
        return AzureDevopsTokenService.AzureDevopsSecrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    public AzureDevopsTokenService azureDevopsTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                                           AzureDevopsTokenService.AzureDevopsSecrets secrets,
                                                           @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String baseUrl) {
        return AzureDevopsTokenService.builder().mapper(mapper).redirectHost(baseUrl).httpClient(httpClient).secrets(secrets)
                .build();
    }

    @Bean
    public SlackTokenService.SlackSecrets slackSecrets(@Value("${SLACK_CLIENT_ID}") String clientId,
                                                       @Value("${SLACK_CLIENT_SECRET}") String clientSecret) {
        return SlackTokenService.SlackSecrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    public SlackTokenService slackTokenService(OkHttpClient okHttpClient, ObjectMapper mapper,
                                               SlackTokenService.SlackSecrets slackSecrets,
                                               @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String redirectHost) {
        return new SlackTokenService(okHttpClient, mapper, slackSecrets, redirectHost);
    }

    @Bean
    public MSTeamsTokenService.MSTeamsSecrets msTeamsSecrets(@Value("${MS_TEAMS_CLIENT_ID}") String clientId,
                                                             @Value("${MS_TEAMS_CLIENT_SECRET}") String clientSecret) {
        return MSTeamsTokenService.MSTeamsSecrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    MSTeamsTokenService msTeamsTokenService(OkHttpClient okHttpClient, ObjectMapper mapper,
                                            MSTeamsTokenService.MSTeamsSecrets msTeamsSecrets,
                                            @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String redirectHost) {
        return new MSTeamsTokenService(okHttpClient, mapper, msTeamsSecrets, redirectHost);
    }

    @Bean
    public GithubTokenService githubTokenService(OkHttpClient okHttpClient, ObjectMapper mapper,
                                                 GithubTokenService.GithubSecrets githubSecret) {
        return new GithubTokenService(okHttpClient, mapper, githubSecret);
    }

    @Bean
    public BitbucketTokenService bitbucketService(OkHttpClient httpClient, ObjectMapper mapper,
                                                  BitbucketTokenService.Secrets secrets,
                                                  @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String baseUrl) {
        return BitbucketTokenService.builder().mapper(mapper).redirectHost(baseUrl).httpClient(httpClient).secrets(secrets).build();
    }

    @Bean
    public GitlabTokenService gitlabService(OkHttpClient httpClient, ObjectMapper mapper,
                                            GitlabTokenService.Secrets secrets,
                                            @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String baseUrl) {
        return GitlabTokenService.builder().mapper(mapper).redirectHost(baseUrl).httpClient(httpClient).secrets(secrets)
                .build();
    }

    @Bean
    public SalesforceTokenService.SalesforceSecrets salesForceSecrets(@Value("${SALESFORCE_CLIENT_ID:client_id}") String clientId,
                                                                      @Value("${SALESFORCE_CLIENT_SECRET:client_secret}") String clientSecret) {
        return SalesforceTokenService.SalesforceSecrets.builder().clientId(clientId).clientSecret(clientSecret).build();
    }

    @Bean
    public SalesforceTokenService salesForceTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                                         SalesforceTokenService.SalesforceSecrets secrets,
                                                         @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String baseUrl) {
        return SalesforceTokenService.builder()
                .mapper(mapper)
                .redirectHost(baseUrl)
                .httpClient(httpClient)
                .secrets(secrets)
                .build();
    }

    @Bean
    public CxSastTokenService cxSastTokenService(OkHttpClient httpClient, ObjectMapper mapper,
                                                 CxSastTokenService.Secrets secrets,
                                                 @Value("${OAUTH_BASE_URL:https://app.levelops.io}") String baseurl) {
        return CxSastTokenService.builder()
                .mapper(mapper)
                .redirectHost(baseurl)
                .httpClient(httpClient)
                .secrets(secrets)
                .build();
    }

    @Bean
    public CxSastTokenService.Secrets cxSastSecrets(@Value("${CXSAST_CLIENT_ID}") String clientId,
                                                    @Value("${CXSAST_CLIENT_SECRET}") String clientSecret) {
        return CxSastTokenService.Secrets.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    @Bean
    public ServicenowTokenService.ServicenowSecrets servicenowSecrets(@Value("${SERVICENOW_CLIENT_ID:client_id}") String clientId,
                                                                      @Value("${SERVICENOW_CLIENT_SECRET:client_secret}") String clientSecret) {
        return ServicenowTokenService.ServicenowSecrets.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    @Bean
    public ServicenowTokenService servicenowTokenService(OkHttpClient httpClient, ObjectMapper mapper, ServicenowTokenService.ServicenowSecrets secrets) {

        return ServicenowTokenService.builder()
                .httpClient(httpClient)
                .mapper(mapper)
                .secrets(secrets)
                .build();
    }

    @Bean
    public GithubAppTokenService githubAppTokenService(
            OkHttpClient httpClient,
            ObjectMapper mapper,
            @Value("${SEI_GHA_ID:}") String seiGhaId,
            @Value("${SEI_GHA_PEM_PRIVATE_KEY:}") String seiGhaPemPrivateKey
    ) {
        return new GithubAppTokenService(httpClient, mapper, seiGhaId, seiGhaPemPrivateKey);
    }
}
