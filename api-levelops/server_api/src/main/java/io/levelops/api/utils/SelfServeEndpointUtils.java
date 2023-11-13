package io.levelops.api.utils;

import io.levelops.web.exceptions.ForbiddenException;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class SelfServeEndpointUtils {

    public static final Set<String> INTERNAL_EMAIL_DOMAINS = Set.of("harness.io", "propelo.ai", "levelops.io");

    public static void validateUser(String userEmail) throws ForbiddenException {
        if (!isUserEmailInternal(userEmail)) {
            throw new ForbiddenException("User '" + userEmail + "' is not authorized to perform this action.");
        }
    }

    private static boolean isUserEmailInternal(String userEmail) {
        String email = StringUtils.defaultString(userEmail);
        return INTERNAL_EMAIL_DOMAINS.stream()
                .anyMatch(domain -> email.endsWith("@" + domain));
    }

}
