package io.levelops.commons.databases.services.dev_productivity.models;

import lombok.Getter;

@Getter
public enum ScmActivityType {
    PRS_CREATED,
    PRS_MERGED,
    PRS_CLOSED,
    PRS_COMMENTS,
    COMMITS_CREATED;
}
