package io.levelops.ingestion.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TriggerType {
    HARNESSNG("harnessng"),
    JIRA("jira"),
    JIRA_CLEANUP("jira_cleanup"),
    GITHUB("github");
    private final String type;
}
