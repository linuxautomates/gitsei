package io.levelops.commons.config;

import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings;
import io.levelops.commons.utils.CommaListSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CicdArtifactCorrelationConfig {

    @Bean
    public CicdArtifactCorrelationSettings cicdArtifactCorrelationSettings(
            @Value("${CICD_ARTIFACT_CORRELATION__READ_PAGE_SIZE:10000}") Integer readPageSize,
            @Value("${CICD_ARTIFACT_CORRELATION__WRITE_PAGE_SIZE:500}") Integer writePageSize,
            @Value("${CICD_ARTIFACT_CORRELATION__IDENTITY__DEFAULT:true}") Boolean correlationIdentityDefault,
            @Value("${CICD_ARTIFACT_CORRELATION__IDENTITY__TENANTS:}") String correlationIdentityTenants,
            @Value("${CICD_ARTIFACT_CORRELATION__NAME_QUALIFIER__DEFAULT:false}") Boolean correlationNameQualifierDefault,
            @Value("${CICD_ARTIFACT_CORRELATION__NAME_QUALIFIER__TENANTS:}") String correlationNameQualifierTenants,
            @Value("${CICD_ARTIFACT_CORRELATION__NAME_QUALIFIER_LOCATION__DEFAULT:true}") Boolean correlationNameQualifierLocationDefault,
            @Value("${CICD_ARTIFACT_CORRELATION__NAME_QUALIFIER_LOCATION__TENANTS:}") String correlationNameQualifierLocationTenants,
            @Value("${CICD_ARTIFACT_CORRELATION__HASH__DEFAULT:true}") Boolean correlationHashDefault,
            @Value("${CICD_ARTIFACT_CORRELATION__HASH__TENANTS:}") String correlationHashTenants
    ) {
        return CicdArtifactCorrelationSettings.builder()
                .readPageSize(readPageSize)
                .writePageSize(writePageSize)
                .correlationIdentityDefault(correlationIdentityDefault)
                .correlationIdentityTenants(CommaListSplitter.splitToSet(correlationIdentityTenants))
                .correlationNameQualifierDefault(correlationNameQualifierDefault)
                .correlationNameQualifierTenants(CommaListSplitter.splitToSet(correlationNameQualifierTenants))
                .correlationNameQualifierLocationDefault(correlationNameQualifierLocationDefault)
                .correlationNameQualifierLocationTenants(CommaListSplitter.splitToSet(correlationNameQualifierLocationTenants))
                .correlationHashDefault(correlationHashDefault)
                .correlationHashTenants(CommaListSplitter.splitToSet(correlationHashTenants))
                .build();
    }
}
