package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketUserConvertersTest {

    @Test
    public void name() {
        DbScmUser user = BitbucketUserConverters.fromBitbucketCommit("1", BitbucketCommit.builder()
                .author(BitbucketCommit.CommitAuthor.builder()
                        .raw("Maxime Bellier <maxime.bellier@harness.io>")
                        .build())
                .build());
        assertThat(user.getDisplayName()).isEqualTo("Maxime Bellier");
        assertThat(user.getCloudId()).isEqualTo("maxime.bellier@harness.io");

        user = BitbucketUserConverters.fromBitbucketCommit("1", BitbucketCommit.builder()
                .author(BitbucketCommit.CommitAuthor.builder()
                        .raw("Maxime Bellier")
                        .build())
                .build());
        assertThat(user.getDisplayName()).isEqualTo("Maxime Bellier");
        assertThat(user.getCloudId()).isEqualTo("Maxime Bellier");
    }
}