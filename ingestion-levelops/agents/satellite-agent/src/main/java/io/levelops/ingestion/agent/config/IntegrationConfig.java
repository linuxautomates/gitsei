package io.levelops.ingestion.agent.config;

import java.time.ZoneId;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
public class IntegrationConfig {

    @Bean("allowUnsafeSSLJira")
    public Boolean allowUnsafeSSLJira(SatelliteConfigFileProperties configProperties) {
        return BooleanUtils.isTrue(configProperties.getJira().getAllowUnsafeSsl());
    }

    @Bean("jiraProject")
    public String jiraProject(SatelliteConfigFileProperties configProperties) {
        return StringUtils.defaultString(configProperties.getJira().getProject());
    }

    @Bean("jiraRateLimitPerSecond")
    public double jiraRateLimitPerSecond(SatelliteConfigFileProperties configProperties) {
        double rateLimit = MoreObjects.firstNonNull(configProperties.getJira().getRateLimitPerSecond(), 0.);
        if (rateLimit > 0) {
            log.info("Jira rate limit enabled: {} request(s) per second", rateLimit);
        }
        return rateLimit;
    }

    @Bean("githubThrottlingMs")
    public int githubThrottlingMs(SatelliteConfigFileProperties configProperties) {
        int throttling = MoreObjects.firstNonNull(configProperties.getGithub().getThrottlingMs(), 0);
        if (throttling > 0) {
            log.info("Github throttling enabled: at most 1 request every {} ms", throttling);
        }
        return throttling;
    }

    @Bean("githubEnableCaching")
    public Boolean githubEnableCaching(SatelliteConfigFileProperties configProperties) {
        return BooleanUtils.isNotFalse(configProperties.getGithub().getEnableCaching());
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
    public int helixMaxFileSize(SatelliteConfigFileProperties configProperties) {
        int maxFileSize = MoreObjects.firstNonNull(configProperties.getHelix().getMaxFileSize(), 3000000); // 3MB
        log.info("Helix max file size: {} bytes", maxFileSize);
        return maxFileSize;
    }

    @Bean("helixZoneId")
    public ZoneId helixZoneId(SatelliteConfigFileProperties configProperties) {
        String zoneIdString = (StringUtils.isBlank(configProperties.getHelix().getZoneId())) ? "America/Los_Angeles" : configProperties.getHelix().getZoneId();
        ZoneId zoneId = ZoneId.of(zoneIdString);
        log.info("Helix zoneId: {}", zoneId);
        return zoneId;
    }
}
