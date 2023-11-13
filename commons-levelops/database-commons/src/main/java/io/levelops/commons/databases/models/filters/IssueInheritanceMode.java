package io.levelops.commons.databases.models.filters;

import lombok.Getter;

@Getter
public enum IssueInheritanceMode {
    ALL_TICKETS(null),
    ALL_TICKETS_AND_IMMEDIATE_CHILDREN("w_parents"),
    ALL_TICKETS_AND_ALL_RECURSIVE_CHILDREN("w_parents"),
    ONLY_LEAF_CHILDREN("w_parents"),
    ALL_TICKETS_AND_ALL_RECURSIVE_EPIC_CHILDREN("w_epic_wi");

    private final String path;

    IssueInheritanceMode(String path) {
        this.path = path;
    }
}
