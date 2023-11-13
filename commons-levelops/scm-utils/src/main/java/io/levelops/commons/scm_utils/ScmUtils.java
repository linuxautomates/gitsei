package io.levelops.commons.scm_utils;

import lombok.extern.log4j.Log4j2;

import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public class ScmUtils {
    private static final Pattern PATTERN_GIT_HTTP_URL = Pattern.compile("\\b\\/(.+\\/.+)\\.git\\b");
    private static final Pattern PATTERN_GIT_SSH_URL = Pattern.compile("^git@.*\\:(.*).git$");
    private static final Pattern PATTERN_AZURE_DEVOPS_HTTP_URL = Pattern.compile("\\b\\/\\_git\\/(.+)");
    public static String parseRepoIdFromScmUrl(final UUID jobId, final String scmUrl) {
        log.debug("scmUrl {}", scmUrl);
        if (StringUtils.isBlank(scmUrl)) {
            log.debug("CiCd Job Id {} scm url is blank.", jobId);
            return null;
        }
        //First try to match with Git Http Url Pattern
        var matcher = PATTERN_GIT_HTTP_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = PATTERN_GIT_SSH_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = PATTERN_AZURE_DEVOPS_HTTP_URL.matcher(scmUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("CiCd Job Id {} scm url may not be supported for format {}", jobId, scmUrl);
        return scmUrl;
    }
}
