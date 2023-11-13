package io.levelops.commons.databases.utils;

import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.reflectoring.diffparser.api.DiffParser;
import io.reflectoring.diffparser.api.UnifiedDiffParser;
import io.reflectoring.diffparser.api.model.Diff;
import io.reflectoring.diffparser.api.model.Line;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class ScmDiffParserUtils {

    public static ChangeVolumeStats fromGitlabDiff(GitlabChange gitlabChange) {
        String rawDiff = gitlabChange.getDiff();
        if (rawDiff.startsWith("@@")) {
            rawDiff = "--- a/" + gitlabChange.getOldPath() + "\n+++ b/" + gitlabChange.getNewPath() + "\n" + gitlabChange.getDiff();
        }
        DiffParser parser = new UnifiedDiffParser();
        List<Diff> diffs = parser.parse(rawDiff.getBytes());
        List<Line> lines = diffs.stream()
                .map(Diff::getHunks)
                .flatMap(Collection::stream)
                .flatMap(hunk -> hunk.getLines().stream())
                .collect(Collectors.toList());
        Map<Line.LineType, List<Line>> diffData = lines.stream().collect(groupingBy(Line::getLineType));
        List<Line> linesAdded = new ArrayList<>(CollectionUtils.emptyIfNull(diffData.get(Line.LineType.TO)));
        List<Line> linesDeleted = new ArrayList<>(CollectionUtils.emptyIfNull(diffData.get(Line.LineType.FROM)));
        return ChangeVolumeStats.builder()
                .fileName(gitlabChange.getNewPath())
                .additions(linesAdded.size())
                .deletions(linesDeleted.size())
                .changes(linesAdded.size() + linesDeleted.size())
                .build();
    }
}
