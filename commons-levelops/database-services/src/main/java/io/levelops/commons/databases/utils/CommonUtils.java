package io.levelops.commons.databases.utils;

import io.levelops.commons.utils.CommaListSplitter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CommonUtils {
    public static final String scmMergeShaEnabledTenants = System.getenv("scm.mergesha.enabled.tenants");
    private static final String ENABLE_FOR_ALL_TENANTS="_all_";

    public static Set<String> scmMergeShaEnabledDBTenants () {
        Set<String> scmMergeShaEnabledDBTenants = CommaListSplitter.splitToStream(scmMergeShaEnabledTenants)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return scmMergeShaEnabledDBTenants;
    }

    public static Boolean getUseMergeShaForCommitsJoinFlag(String company)
    {
        return scmMergeShaEnabledDBTenants().contains(ENABLE_FOR_ALL_TENANTS) | CommonUtils.scmMergeShaEnabledDBTenants().contains(company);
    }
}