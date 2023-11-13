package io.levelops.etl.jobs.user_id_consolidation;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class UserConsolidationGraphUtilsTest {
    private DbScmUser createUser(List<String> emails, String id) {
        return DbScmUser.builder()
                .id(id)
                .integrationId("1")
                .cloudId(RandomStringUtils.random(10))
                .emails(emails)
                .build();
    }


    @Test
    public void testCreateGraph() {
        Map<String, List<String>> emailToUserIds = new HashMap<>();
        Map<String, List<String>> emailGraph = new HashMap<>();
        UserConsolidationGraphUtils.createGraphs(
                List.of(
                        createUser(List.of("a", "b"), "1"),
                        createUser(List.of("b"), "2"),
                        createUser(List.of("c", "d"), "3"),
                        createUser(List.of("a", "e"), "4"),
                        createUser(List.of("f", "g"), "5"),
                        createUser(List.of("g", "h"), "6")
                ),
                emailToUserIds,
                emailGraph
        );

        assertThat(emailToUserIds).containsOnly(
                entry("a", List.of("1", "4")),
                entry("b", List.of("1", "2")),
                entry("c", List.of("3")),
                entry("d", List.of("3")),
                entry("e", List.of("4")),
                entry("f", List.of("5")),
                entry("g", List.of("5", "6")),
                entry("h", List.of("6"))
        );

        assertThat(emailGraph).containsOnly(
                entry("a", List.of("b", "e")),
                entry("b", List.of("a")),
                entry("c", List.of("d")),
                entry("d", List.of("c")),
                entry("e", List.of("a")),
                entry("f", List.of("g")),
                entry("g", List.of("f", "h")),
                entry("h", List.of("g"))
        );
    }

    @Test
    public void testCreateEmptyGraph() {
        Map<String, List<String>> emailToUserIds = new HashMap<>();
        Map<String, List<String>> emailGraph = new HashMap<>();
        UserConsolidationGraphUtils.createGraphs(
                List.of(),
                emailToUserIds,
                emailGraph
        );
        assertThat(emailToUserIds).isEmpty();
        assertThat(emailGraph).isEmpty();
    }

    private void testConnectedComponents(List<DbScmUser> users, List<Set<String>> expected) {
        Map<String, List<String>> emailToUserIds = new HashMap<>();
        Map<String, List<String>> emailGraph = new HashMap<>();
        UserConsolidationGraphUtils.createGraphs(
                users,
                emailToUserIds,
                emailGraph
        );

        var connectedComponent = UserConsolidationGraphUtils.getConnectedComponents(emailGraph);
        assertThat(connectedComponent).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetConnectedComponents() {
        testConnectedComponents(
                List.of(
                        createUser(List.of("a", "b"), "1"),
                        createUser(List.of("b"), "2"),
                        createUser(List.of("c", "d"), "3"),
                        createUser(List.of("a", "e"), "4"),
                        createUser(List.of("f", "g"), "5"),
                        createUser(List.of("g", "h"), "6")
                ),
                List.of(
                        Set.of("a", "b", "e"),
                        Set.of("c", "d"),
                        Set.of("f", "g", "h")
                )
        );

        testConnectedComponents(
                List.of(
                        createUser(List.of("a", "b"), "1"),
                        createUser(List.of("b", "c"), "2"),
                        createUser(List.of("c", "d"), "3"),
                        createUser(List.of("a", "e"), "4"),
                        createUser(List.of("f", "g"), "5"),
                        createUser(List.of("g", "h"), "6")
                ),
                List.of(
                        Set.of("a", "b", "c", "d", "e"),
                        Set.of("f", "g", "h")
                )
        );

        // Test no connected components
        testConnectedComponents(
                List.of(
                        createUser(List.of("a"), "1"),
                        createUser(List.of("b"), "2"),
                        createUser(List.of("c"), "3")
                ),
                List.of(
                        Set.of("a"),
                        Set.of("b"),
                        Set.of("c")
                )
        );

        testConnectedComponents(
                List.of(
                        createUser(List.of("a", "b", "c", "d", "e"), "1"),
                        createUser(List.of("b", "z"), "2"),
                        createUser(List.of("c", "y"), "3"),
                        createUser(List.of("d", "x"), "4"),
                        createUser(List.of("e", "w"), "5"),
                        createUser(List.of("f"), "6"),
                        createUser(List.of("g"), "7")
                ),
                List.of(
                        Set.of("a", "b", "c", "d", "e", "w", "x", "y", "z"),
                        Set.of("f"),
                        Set.of("g")
                )
        );
    }


}