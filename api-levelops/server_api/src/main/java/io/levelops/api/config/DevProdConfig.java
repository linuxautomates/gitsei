package io.levelops.api.config;

import io.levelops.commons.utils.CommaListSplitter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Configuration
public class DevProdConfig {
    @Bean("readDevProductivityV2Enabled")
    public boolean readDevProductivityV2Enabled(@Value("${DEV_PROD_READ_V2_ENABLED:false}") Boolean readsV2Enabled) {
        boolean readDevProductivityV2Enabled = BooleanUtils.isTrue(readsV2Enabled);
        log.info("readDevProductivityV2Enabled = {}", readDevProductivityV2Enabled);
        return readDevProductivityV2Enabled;
    }

    @Bean("parentProfilesEnabledTenants")
    public Set<String> parentProfilesEnabledTenants(@Value("${DEV_PROD_PARENT_PROFILES_ENABLED_TENANTS}") String parentProfilesEnabledTenantsString) {
        log.info("parentProfilesEnabledTenantsString = {}", parentProfilesEnabledTenantsString);
        Set<String> parentProfilesEnabledTenants = CommaListSplitter.splitToStream(parentProfilesEnabledTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("newIntervalsEnabledTenants = {}", parentProfilesEnabledTenants);
        return parentProfilesEnabledTenants;
    }

    @Bean("readDevProductivityV2UserReportTenants")
    public Set<String> readDevProductivityV2UserReportTenants (@Value("${DEV_PROD_READ_V2_USER_REPORT_TENANTS:}") String readDevProductivityV2ReportTenantsString) {
        log.info("readDevProductivityV2ReportTenantsString = {}", readDevProductivityV2ReportTenantsString);
        Set<String> readDevProductivityV2UserReportTenants = CommaListSplitter.splitToStream(readDevProductivityV2ReportTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("readDevProductivityV2UserReportTenants = {}", readDevProductivityV2UserReportTenants);
        return readDevProductivityV2UserReportTenants;
    }

    @Bean("persistDevProductivityV2EventsTenantsBlacklist")
    public Set<String> persistDevProductivityV2EventsTenantsBlacklist (@Value("${DEV_PROD_PERSIST_V2_EVENTS_TENANTS_BLACKLIST:}") String persistDevProductivityV2EventsTenantsBlacklistString) {
        log.info("persistDevProductivityV2EventsTenantsBlacklistString = {}", persistDevProductivityV2EventsTenantsBlacklistString);
        Set<String> persistDevProductivityV2EventsTenantsBlacklist = CommaListSplitter.splitToStream(persistDevProductivityV2EventsTenantsBlacklistString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("persistDevProductivityV2EventsTenantsBlacklist = {}", persistDevProductivityV2EventsTenantsBlacklist);
        return persistDevProductivityV2EventsTenantsBlacklist;
    }

    @Bean("issuePartialCreditJiraTenants")
    public Set<String> issuePartialCreditJiraTenants (@Value("${DEV_PROD_ISSUE_PARTIAL_CREDIT_JIRA_TENANTS:}")  String issuePartialCreditJiraTenantsString) {
        log.info("issuePartialCreditJiraTenantsString = {}", issuePartialCreditJiraTenantsString);
        Set<String> issuePartialCreditJiraTenants = CommaListSplitter.splitToStream(issuePartialCreditJiraTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("issuePartialCreditJiraTenants = {}", issuePartialCreditJiraTenants);
        return issuePartialCreditJiraTenants;
    }

    @Bean("issuePartialCreditWITenants")
    public Set<String> issuePartialCreditWITenants (@Value("${DEV_PROD_ISSUE_PARTIAL_CREDIT_WI_TENANTS:}")  String issuePartialCreditWITenantsString) {
        log.info("issuePartialCreditWITenantsString = {}", issuePartialCreditWITenantsString);
        Set<String> issuePartialCreditWITenants = CommaListSplitter.splitToStream(issuePartialCreditWITenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("issuePartialCreditWITenants = {}", issuePartialCreditWITenants);
        return issuePartialCreditWITenants;
    }

}
