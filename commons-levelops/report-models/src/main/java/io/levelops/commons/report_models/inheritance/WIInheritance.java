package io.levelops.commons.report_models.inheritance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum WIInheritance {
    ALL_TICKETS(null, ""),
    ALL_TICKETS_AND_IMMEDIATE_CHILDREN("w_parents", "ticketAndImmediateChildren"),
    ALL_TICKETS_AND_ALL_RECURSIVE_CHILDREN("w_parents", "ticketAndAllChildren"),
    ONLY_LEAF_CHILDREN("w_parents", "leafChildren"),
    ALL_TICKETS_AND_ALL_RECURSIVE_EPIC_CHILDREN("w_epic_wi", "ticketAndAllEpicChildren");

    private final String path;
    private final String explainText;

    WIInheritance(String path, String explainText) {
        this.path = path;
        this.explainText = explainText;
    }

    @JsonCreator
    @Nullable
    public static WIInheritance fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(WIInheritance.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
