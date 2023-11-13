package io.levelops.ingestion.agent.config;

import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationConfig {

    @Bean("allowUnsafeSSLJira")
    public Boolean allowUnsafeSSLJira() {
        return true;
    }

    @Bean("jiraProject")
    public String jiraProject() {
        return "";
    }

    @Bean("jiraRateLimitPerSecond")
    public Double jiraRateLimitPerSecond() {
        return 0.;
    }

    @Bean("githubThrottlingMs")
    public int githubThrottlingMs() {
        return 10;
    }

    @Bean("githubEnableCaching")
    public Boolean githubEnableCaching() {
        return true;
    }

    @Bean("githubOrgCacheMaxSize")
    public long githubOrgCacheMaxSize() {
        return 100;
    }

    @Bean("githubOrgCacheExpiryInHours")
    public long githubOrgCacheExpiryInHours() {
        return 24;
    }

    @Bean("githubRepoCacheMaxSize")
    public long githubRepoCacheMaxSize() {
        return 20000; // .5kb per repo -> 10MB
    }

    @Bean("githubRepoCacheExpiryInHours")
    public long githubRepoCacheExpiryInHours() {
        return 24;
    }

    @Bean("allowUnsafeSSLGitlab")
    public Boolean allowUnsafeSSLGitlab(){
        return true;
    }

    @Bean("allowUnsafeSSLBlackDuck")
    public Boolean allowUnsafeSSLBlackDuck(){
        return true;
    }

    @Bean("allowUnsafeSSLCoverity")
    public Boolean allowUnsafeSSLCoverity(){
        return true;
    }

    @Bean("allowUnsafeSSLBitbucketServer")
    public Boolean allowUnsafeSSLBitbucketServer(){
        return true;
    }

    @Bean("helixMaxFileSize")
    public int helixMaxFileSize() {
        return 3000000; // 3 MB
    }

    @Bean("helixZoneId")
    public ZoneId helixZoneId() {
        return ZoneId.of("America/Los_Angeles");
    }
}
