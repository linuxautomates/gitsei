package io.levelops.commons.databases.services.business_alignment;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BaJiraReportSubType {
    public static String generateReportSubType(String uriUnit, String ticketCategorizationSchemeId, String baCategory) {
        String reportSubType = uriUnit + "_" + ticketCategorizationSchemeId + "_" + baCategory;
        log.info("uriUnit {}, ticketCategorizationSchemeId {}, baCategory {}, reportSubType {}", uriUnit, ticketCategorizationSchemeId, baCategory, reportSubType);
        return reportSubType;
    }
}
