package io.levelops.integrations.bitbucket.models;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BitbucketUtils {
    public static final SimpleDateFormat BITBUCKET_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    public static final SimpleDateFormat BITBUCKET_COMMIT_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
//    static {
//        BITBUCKET_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
//    }
}
