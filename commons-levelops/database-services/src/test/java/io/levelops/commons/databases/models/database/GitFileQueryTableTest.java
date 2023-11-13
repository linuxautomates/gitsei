package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MinMaxPriorityQueue;
import io.levelops.commons.databases.models.database.temporary.TempGitFileChange;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.GitFileChangeQueryTable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GitFileQueryTableTest {
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
        try (GitFileChangeQueryTable gcqt = new GitFileChangeQueryTable(ds, "test", "gitfiles", m)) {
            gcqt.createTempTable();
            List<TempGitFileChange> data = new ArrayList<>();
            Set<String> idsSeen = new HashSet<>();

            repositories.forEach(repo -> repo.getEvents().stream()
                    .filter(event -> "PushEvent".equalsIgnoreCase(event.getType()))
                    .forEach(event -> event.getCommits().forEach(commit -> {
                        if (idsSeen.contains(commit.getSha()))
                            return;
                        idsSeen.add(commit.getSha());
                        data.addAll(TempGitFileChange.fromGithubCommit(commit, repo.getId()));
                    })));

            gcqt.insertRows(data);
            assertThat(gcqt.countRows(Collections.emptyList(), false)).isGreaterThan(0);

            List<String> repos = gcqt.distinctValues(new QueryField("repo_name",
                    QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                    null));

            List<String> files = gcqt.distinctValues(new QueryField("file_name",
                    QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                    null));
            assertThat(!files.isEmpty());

            MinMaxPriorityQueue<ImmutablePair<Integer, String>> filesChanged = MinMaxPriorityQueue
                    .orderedBy((Comparator<ImmutablePair<Integer, String>>) (o1, o2) -> o2.getLeft() - o1.getLeft())
                    .maximumSize(10).create();

            for (String repo : repos) {
                for (String file : files) {
                    Integer ct = gcqt.countRows(List.of(new QueryGroup(List.of(
                            new QueryField("file_name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(), file),
                            new QueryField("repo_name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(), repo)),
                            QueryGroup.GroupOperator.AND)), false);
                    if (ct > 0) {
                        List<TempGitFileChange> fileChanges = gcqt.getRows(List.of(new QueryGroup(List.of(
                                new QueryField("file_name", QueryField.FieldType.STRING,
                                        QueryField.Operation.EXACT_MATCH, Collections.emptyList(), file),
                                new QueryField("repo_name", QueryField.FieldType.STRING,
                                        QueryField.Operation.EXACT_MATCH, Collections.emptyList(), repo)),
                                QueryGroup.GroupOperator.AND)), false,0, 100000);
                        int adds = fileChanges.stream().mapToInt(TempGitFileChange::getAdditions).sum();
                        int changes = fileChanges.stream().mapToInt(TempGitFileChange::getChanges).sum();
                        int deletes = fileChanges.stream().mapToInt(TempGitFileChange::getDeletions).sum();
                        System.out.println(ct + " name: " + file + " add: " + adds + " change: "
                                + changes + " deletions: " + deletes);
                        filesChanged.add(ImmutablePair.of(ct, file));
                    }
                }
            }

            System.out.println(filesChanged.toString());
        }
    }
}
