package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketUser;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.levelops.commons.databases.models.database.scm.DbScmUser.UNKNOWN;

public class BitbucketUserConverters {

    private static final Pattern RAW_GIT_USER_REGEX = Pattern.compile("([^<]*)(<([^>]*)>)?"); // User Name <email>

    @Nonnull
    public static DbScmUser fromBitbucketUser(String integrationId, @Nullable BitbucketUser user) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(user)
                        .map(BitbucketUser::getUuid)
                        .orElse(UNKNOWN))
                .displayName(Optional.ofNullable(user)
                        .map(BitbucketUser::getDisplayName)
                        .orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(user)
                        .map(BitbucketUser::getDisplayName)
                        .orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromBitbucketCommit(String integrationId, BitbucketCommit source) {
        if (source.getAuthor() != null) {
            if (source.getAuthor().getUser() != null) {
                return fromBitbucketUser(integrationId, source.getAuthor().getUser());
            }
            String raw = StringUtils.defaultString(source.getAuthor().getRaw());
            Matcher matcher = RAW_GIT_USER_REGEX.matcher(raw);
            if (matcher.matches()) {
                String displayName = StringUtils.defaultIfBlank(matcher.group(1), UNKNOWN).trim();
                String cloudId = StringUtils.firstNonBlank(matcher.group(3), displayName, UNKNOWN).trim();
                return DbScmUser.builder()
                        .integrationId(integrationId)
                        .cloudId(cloudId)
                        .displayName(displayName)
                        .originalDisplayName(displayName)
                        .build();
            }
        }
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(UNKNOWN)
                .displayName(UNKNOWN)
                .originalDisplayName(UNKNOWN)
                .build();
    }

}
