package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GerritCommitConvertersTest {

    @Test
    public void parseCommit() throws IOException {
        ChangeInfo changeInfo = ResourceUtils.getResourceAsObject("gerrit/gerrit_change.json", ChangeInfo.class);
        DbScmCommit commit = GerritCommitConverters.parseCommitByRevisionId("1", changeInfo, changeInfo.getCurrentRevision());
        DefaultObjectMapper.prettyPrint(commit);

        DbScmCommit expected = ResourceUtils.getResourceAsObject("gerrit/expected_commit.json", DbScmCommit.class);
        assertThat(commit).isEqualTo(expected);

        Optional<DbScmCommit> opt = GerritCommitConverters.parseCommit("1", changeInfo);
        assertThat(opt).isEmpty();
    }

    @Test
    public void parseCommit2() throws IOException {
        ChangeInfo changeInfo = ResourceUtils.getResourceAsObject("gerrit/gerrit_change2.json", ChangeInfo.class);
        DbScmCommit commit = GerritCommitConverters.parseCommitByRevisionId("1", changeInfo, changeInfo.getCurrentRevision());
        DefaultObjectMapper.prettyPrint(commit);

        DbScmCommit expected = ResourceUtils.getResourceAsObject("gerrit/expected_commit2.json", DbScmCommit.class);
        assertThat(commit).isEqualTo(expected);

        Optional<DbScmCommit> opt = GerritCommitConverters.parseCommit("1", changeInfo);
        assertThat(opt).isPresent();
        assertThat(opt.orElseThrow()).isEqualTo(expected);
    }
}