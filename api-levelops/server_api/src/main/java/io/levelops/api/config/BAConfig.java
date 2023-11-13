package io.levelops.api.config;

import io.levelops.commons.services.business_alignment.es.result_converter.composite.BACompositeESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.result_converter.terms.BATermsESResultConverterFactory;
import io.levelops.commons.utils.CommaListSplitter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Configuration
public class BAConfig {
    @Bean("baReadTicketCountESTenants")
    public Set<String> baReadTicketCountESTenants (@Value("${BA_READ_TICKET_COUNT_ES_TENANTS:}") String baReadTicketCountESTenantsString) {
        log.info("baReadTicketCountESTenantsString = {}", baReadTicketCountESTenantsString);
        Set<String> baReadTicketCountESTenants = CommaListSplitter.splitToStream(baReadTicketCountESTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("baReadTicketCountESTenants = {}", baReadTicketCountESTenants);
        return baReadTicketCountESTenants;
    }

    @Bean("baReadStoryPointsESTenants")
    public Set<String> baReadStoryPointsESTenants (@Value("${BA_READ_STORY_POINTS_ES_TENANTS:}") String baReadStoryPointsESTenantsString) {
        log.info("baReadStoryPointsESTenantsString = {}", baReadStoryPointsESTenantsString);
        Set<String> baReadStoryPointsESTenants = CommaListSplitter.splitToStream(baReadStoryPointsESTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("baReadStoryPointsESTenants = {}", baReadStoryPointsESTenants);
        return baReadStoryPointsESTenants;
    }

    @Bean("baReadTicketTimeESTenants")
    public Set<String> baReadTicketTimeESTenants (@Value("${BA_READ_TICKET_TIME_ES_TENANTS:}") String baReadTicketTimeESTenantsString) {
        log.info("baReadTicketTimeESTenantsString = {}", baReadTicketTimeESTenantsString);
        Set<String> baReadTicketTimeESTenants = CommaListSplitter.splitToStream(baReadTicketTimeESTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        log.info("baReadTicketTimeESTenants = {}", baReadTicketTimeESTenants);
        return baReadTicketTimeESTenants;
    }

    @Bean
    public BATermsESResultConverterFactory baTermsESResultConverterFactory () {
        return new BATermsESResultConverterFactory();
    }
    @Bean
    public BACompositeESResultConverterFactory baCompositeESResultConverterFactory () {
        return new BACompositeESResultConverterFactory();
    }
}
