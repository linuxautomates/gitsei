package io.levelops.commons.databases.utils;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IssueMgmtCustomFieldUtils {

    private static final Set<String> CUSTOM_FIELD_KEYS_WHITELIST = Stream.of(
                    "Microsoft.VSTS.Scheduling.Effort",
                    "Microsoft.VSTS.Common.ValueArea",
                    "Microsoft.VSTS.Scheduling.StartDate",
                    "Microsoft.VSTS.Scheduling.TargetDate",
                    "Microsoft.VSTS.Scheduling.DueDate"
            )
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    private static final Set<String> CUSTOM_FIELD_KEY_PREFIXES_WHITELIST = Set.of("custom.", "gcc.", "dw.");


    public static boolean isCustomField(@Nullable String fieldKey) {
        String key = StringUtils.defaultString(fieldKey).toLowerCase();
        return CUSTOM_FIELD_KEYS_WHITELIST.contains(key) || CUSTOM_FIELD_KEY_PREFIXES_WHITELIST.stream().anyMatch(key::startsWith);
    }

}
