package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.temporary.TempGitCommit;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.GitCommitQueryTable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GitCommitQueryTableTest {
    private final ObjectMapper m = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private PGSimpleDataSource simpleDataSource() {
        return (PGSimpleDataSource) pg.getEmbeddedPostgres().getPostgresDatabase();
    }

    @Test
    public void testBatchUpsert() throws SQLException, IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/githubrepo.json");
        List<GithubRepository> repositories = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, GithubRepository.class));
        PGSimpleDataSource ds = simpleDataSource();
        try (GitCommitQueryTable gcqt = new GitCommitQueryTable(ds, "test", "gitcommits", m)) {
            gcqt.createTempTable();
            List<TempGitCommit> data = new ArrayList<>();
            Set<String> idsSeen = new HashSet<>();

            repositories.forEach(repo -> repo.getEvents().stream()
                    .filter(event -> "PushEvent".equalsIgnoreCase(event.getType()))
                    .forEach(event -> event.getCommits().forEach(commit -> {
                        if (idsSeen.contains(commit.getSha()))
                            return;
                        idsSeen.add(commit.getSha());
                        data.add(TempGitCommit.fromGithubCommit(commit, repo.getId()));
                    })));

            gcqt.insertRows(data);
            assertThat(gcqt.countRows(Collections.emptyList(), false)).isGreaterThan(0);

            List<String> repos = gcqt.distinctValues(new QueryField("repo_name",
                    QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                    null));

            List<String> users = gcqt.distinctValues(new QueryField("author_name",
                    QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                    null));
            assertThat(!users.isEmpty());

            for (String repo : repos) {
                for (String user : users) {
                    assertThat(gcqt.countRows(List.of(new QueryGroup(List.of(
                            new QueryField("author_name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(), user),
                            new QueryField("repo_name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(), repo)),
                            QueryGroup.GroupOperator.AND)), false)).isGreaterThan(-1);
                }
            }
        }
    }
}
