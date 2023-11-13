package io.levelops.commons.tenant_management.models;

import lombok.Getter;

@Getter
public enum OffsetType {
    JIRA,
    WI,
    SCM_COMMIT,
    SCM_PR;
}
