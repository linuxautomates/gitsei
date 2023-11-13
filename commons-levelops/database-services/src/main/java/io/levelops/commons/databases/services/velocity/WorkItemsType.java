package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum WorkItemsType {
    JIRA,
    WORK_ITEM,
    JIRA_AND_WORK_ITEM,
    NONE;

    @JsonCreator
    @Nullable
    public static WorkItemsType fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(WorkItemsType.class, st);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

    public static WorkItemsType fromListRequest(DefaultListRequest filter) {
        return WorkItemsType.fromString(filter.getFilterValue("work_items_type", String.class).orElse(null));
    }
}
