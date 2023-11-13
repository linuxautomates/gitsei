package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GerritPullRequestConvertersTest {

    @Test
    public void testFromGerritChange() throws IOException {
        ChangeInfo changeInfo = ResourceUtils.getResourceAsObject("gerrit/gerrit_change.json", ChangeInfo.class);
        DbScmPullRequest dbScmPullRequest = GerritPullRequestConverters.parsePullRequest("1", changeInfo);
        DefaultObjectMapper.prettyPrint(dbScmPullRequest);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("gerrit/expected_pr.json", DbScmPullRequest.class);
        assertThat(dbScmPullRequest).isEqualTo(expected);
    }

    @Test
    public void testFromGerritChange2() throws IOException {
        ChangeInfo changeInfo = ResourceUtils.getResourceAsObject("gerrit/gerrit_change2.json", ChangeInfo.class);
        DbScmPullRequest dbScmPullRequest = GerritPullRequestConverters.parsePullRequest("1", changeInfo);
        DefaultObjectMapper.prettyPrint(dbScmPullRequest);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("gerrit/expected_pr2.json", DbScmPullRequest.class);
        assertThat(dbScmPullRequest).isEqualTo(expected);
    }
}