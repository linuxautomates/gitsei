package io.levelops.commons.services.business_alignment.es.result_converter.composite;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class CompositeAggsAfterKeyUtils {
    private static final Pattern P = Pattern.compile("^\"|\"$");
    public static String formatStringData(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        return P.matcher(input).replaceAll("");
    }
}
