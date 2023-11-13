package io.levelops.integrations.droneci.source;


import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DroneCIEnrichRepoDataSourceTest {

    private DroneCIEnrichRepoData createRepo(String slug) {
        return DroneCIEnrichRepoData.builder()
                .slug(slug)
                .build();
    }

    @Test
    public void testRepoFilterPredicate() {
        var repos = List.of("propelo/commons", "propelo/server-api");
        var excludeRepos = List.of("propelo/commons", "propelo/junk");
        var includeExcludePredicate = new DroneCIEnrichRepoDataSource.RepoFilterPredicate(repos, excludeRepos);
        var includeOnlyPredicate = new DroneCIEnrichRepoDataSource.RepoFilterPredicate(repos, null);
        var excludeOnlyPredicate = new DroneCIEnrichRepoDataSource.RepoFilterPredicate(null, excludeRepos);
        var noFilterPredicate = new DroneCIEnrichRepoDataSource.RepoFilterPredicate(null, null);
        var droneRepos = List.of(
                createRepo("propelo/commons"),
                createRepo("propelo/server-api"),
                createRepo("propelo/internal-api"),
                createRepo("propelo/ingestion"),
                createRepo("propelo/junk")
        );

        assertThat(droneRepos.stream().filter(noFilterPredicate).collect(Collectors.toList())).isEqualTo(
                droneRepos
        );
        assertThat(droneRepos.stream().filter(includeOnlyPredicate).map(DroneCIEnrichRepoData::getSlug).collect(Collectors.toList())).isEqualTo(
                List.of("propelo/commons", "propelo/server-api")
        );
        assertThat(droneRepos.stream().filter(excludeOnlyPredicate).map(DroneCIEnrichRepoData::getSlug).collect(Collectors.toList())).isEqualTo(
                List.of("propelo/server-api", "propelo/internal-api", "propelo/ingestion")
        );
        assertThat(droneRepos.stream().filter(includeExcludePredicate).map(DroneCIEnrichRepoData::getSlug).collect(Collectors.toList())).isEqualTo(
                List.of("propelo/server-api")
        );
    }
}