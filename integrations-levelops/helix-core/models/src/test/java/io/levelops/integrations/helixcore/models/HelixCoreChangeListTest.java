package io.levelops.integrations.helixcore.models;

import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.levelops.integrations.helixcore.models.HelixCoreChangeListUtils.extractDiff;
import static io.levelops.integrations.helixcore.models.HelixCoreChangeListUtils.extractDiffForChanges;

public class HelixCoreChangeListTest {

    private static final String ADDED_LINES_PATTERN = "add (.*) lines";
    private static final String DELETED_LINES_PATTERN = "deleted (.*) lines";
    private static final String CHANGED_LINES_PATTERN = "changed (.*) lines";

    @Test
    public void testChangeVolume() throws IOException {
        String differences = ResourceUtils.getResourceAsString("helix/changelist_differences.txt");
        List<String> diffLines = differences.lines()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        int i = 0;
        Assertions.assertThat(extractDiff(ADDED_LINES_PATTERN, diffLines.get(i + 1))).isEqualTo(0);
        Assertions.assertThat(extractDiff(DELETED_LINES_PATTERN, diffLines.get(i + 2))).isEqualTo(0);
        Assertions.assertThat(extractDiff(CHANGED_LINES_PATTERN, diffLines.get(i + 3))).isEqualTo(0);
        Assertions.assertThat(extractDiff(ADDED_LINES_PATTERN, diffLines.get(i + 5))).isEqualTo(5);
        Assertions.assertThat(extractDiff(DELETED_LINES_PATTERN, diffLines.get(i + 6))).isEqualTo(1);
        Assertions.assertThat(extractDiff(CHANGED_LINES_PATTERN, diffLines.get(i + 7))).isEqualTo(0);

        differences = ResourceUtils.getResourceAsString("helix/changelist_differences_2.txt");
        diffLines = differences.lines()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        Assertions.assertThat(extractDiff(ADDED_LINES_PATTERN, diffLines.get(i + 1))).isEqualTo(1);
        Assertions.assertThat(extractDiff(DELETED_LINES_PATTERN, diffLines.get(i + 2))).isEqualTo(0);
        Assertions.assertThat(extractDiff(CHANGED_LINES_PATTERN, diffLines.get(i + 3))).isEqualTo(0);
        Assertions.assertThat(extractDiff(ADDED_LINES_PATTERN, diffLines.get(i + 5))).isEqualTo(4);
        Assertions.assertThat(extractDiff(DELETED_LINES_PATTERN, diffLines.get(i + 6))).isEqualTo(0);
        Assertions.assertThat(extractDiff(CHANGED_LINES_PATTERN, diffLines.get(i + 7))).isEqualTo(0);
        Assertions.assertThat(extractDiff(ADDED_LINES_PATTERN, diffLines.get(i + 9))).isEqualTo(4);
        Assertions.assertThat(extractDiff(DELETED_LINES_PATTERN, diffLines.get(i + 10))).isEqualTo(0);
        Assertions.assertThat(extractDiff(CHANGED_LINES_PATTERN, diffLines.get(i + 11))).isEqualTo(0);
    }

    @Test
    public void testChangeChunks() throws IOException {
        String differences = ResourceUtils.getResourceAsString("helix/changelist_differences_2.txt");
        List<String> diffLines = differences.lines()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        int i = 10;
        ImmutablePair<Integer, Integer> diffForChanges = extractDiffForChanges(diffLines.get(++i));
        Assertions.assertThat(diffForChanges.getRight()).isEqualTo(3);
        Assertions.assertThat(diffForChanges.getLeft()).isEqualTo(5);
    }
}
