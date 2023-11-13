package io.levelops.integrations.gitlab.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GitlabUtils {
//    public static final SimpleDateFormat GITLAB_DATE_FORMATTER =
//            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSXXX", Locale.US);
//    public static final SimpleDateFormat GITLAB_COMMIT_DATE_FORMATTER =
//            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sssXXX", Locale.US);


    public static SimpleDateFormat buildDateFormatter() {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSXXX", Locale.US);
        return formatter;
    }
    public static String format(Date d) {
        final SimpleDateFormat formatter = GitlabUtils.buildDateFormatter();
        return formatter.format(d);
    }
}
