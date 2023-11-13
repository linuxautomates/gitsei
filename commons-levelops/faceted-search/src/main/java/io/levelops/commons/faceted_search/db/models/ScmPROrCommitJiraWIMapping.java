package io.levelops.commons.faceted_search.db.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class ScmPROrCommitJiraWIMapping {
    private final UUID prOrCommitId;

    private final List<String> workItemIds;
}
