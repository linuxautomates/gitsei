package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.scm.DbScmFile.NO_FILE_TYPE;

public class GerritFileConverters {

    public static List<DbScmFile> parseCommitFiles(String integrationId,
                                                   ChangeInfo source,
                                                   String revisionId) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(source, "source cannot be null.");
        Validate.notBlank(revisionId, "revisionId cannot be null or empty.");

        RevisionInfo revision = MapUtils.emptyIfNull(source.getRevisions()).get(revisionId);
        if (revision == null) {
            return Collections.emptyList();
        }
        String repoId = StringUtils.defaultString(source.getProject());
        Long committedAt = DateUtils.toEpochSecond(revision.getCreated());
        return MapUtils.emptyIfNull(revision.getFiles()).entrySet()
                .stream()
                .map(entry -> DbScmFile.builder()
                        .filename(entry.getKey())
                        .repoId(repoId)
                        .filetype(StringUtils.defaultIfBlank(FilenameUtils.getExtension(entry.getKey()), NO_FILE_TYPE))
                        .project(repoId)
                        .integrationId(integrationId)
                        .totalAdditions(Long.valueOf(MoreObjects.firstNonNull(entry.getValue().getLinesInserted(), 0)))
                        .totalDeletions(Long.valueOf(MoreObjects.firstNonNull(entry.getValue().getLinesDeleted(), 0)))
                        .totalChanges(0L)
                        .fileCommits(List.of(parseFileCommit(entry.getValue(), revisionId, committedAt)))
                        .build())
                .collect(Collectors.toList());
    }

    private static DbScmFileCommit parseFileCommit(RevisionInfo.FileInfo source,
                                                  String commitSha,
                                                  Long committedAt) {
        int linesInserted = MoreObjects.firstNonNull(source.getLinesInserted(), 0);
        int linesDeleted = MoreObjects.firstNonNull(source.getLinesDeleted(), 0);
        return DbScmFileCommit.builder()
                .addition(linesInserted)
                .change(linesDeleted + linesInserted)
                .deletion(linesDeleted)
                .commitSha(commitSha)
                .committedAt(committedAt)
                .build();
    }

}
