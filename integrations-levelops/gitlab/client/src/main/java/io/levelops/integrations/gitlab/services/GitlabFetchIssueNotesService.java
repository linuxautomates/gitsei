package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabIssueNote;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class can be used for enriching a {@link GitlabIssue} with {@link GitlabIssueNote}.
 */
@Log4j2
public class GitlabFetchIssueNotesService {
    public Stream<GitlabIssueNote> getIssueNotes(GitlabClient client, String projectID, GitlabIssue issue, int perPage) {
        final String issueIID = String.valueOf(issue.getIid());
        MutableInt issuesCount = new MutableInt(0);
        return client.streamIssueNotes(projectID, issueIID, perPage)
                .filter(Objects::nonNull)
                .peek(c -> {
                    issuesCount.increment();
                    if (issuesCount.getValue() % 50 == 0) {
                        log.info("Processed issues for projects={}: MRsCount={}", projectID,
                                issuesCount.getValue());
                    }
                });
    }
}
