package io.levelops.aggregations_shared.models;

import io.levelops.commons.utils.CommaListSplitter;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
@Builder
@lombok.Value
public class IntegrationWhitelistEntry {
    String tenantId;
    String integrationId;

    public static IntegrationWhitelistEntry of(String tenantId, String integrationId) {
        return IntegrationWhitelistEntry.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build();
    }

    static public IntegrationWhitelistEntry fromString(String str) {
        String[] parts = str.split("::");
        if (parts.length != 2) {
            log.error("Incorrect integration whitelist entry found: {}", str);
            return null;
        }
        return IntegrationWhitelistEntry.builder()
                .tenantId(parts[0])
                .integrationId(parts[1])
                .build();
    }

    public static List<IntegrationWhitelistEntry> fromCommaSeparatedString(String str) {
        var integrationIdStrWhitelist = CommaListSplitter.split(str);
        return integrationIdStrWhitelist.stream()
                .map(IntegrationWhitelistEntry::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
