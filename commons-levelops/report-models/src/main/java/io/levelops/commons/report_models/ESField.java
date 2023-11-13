package io.levelops.commons.report_models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum ESField {
    w_id,
    w_workitem_id,
    w_is_active,
    w_integration_id,
    w_project,
    w_components,
    w_labels,
    w_assignee,
    w_reporter,
    w_workitem_type,
    w_first_assignee,
    w_desc_size,
    w_hops,
    w_bounces,
    w_num_attachments,
    w_priority_order,
    w_custom_field;

    @JsonCreator
    @Nullable
    public static ESField fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ESField.class, value);
    }

    public String readableString() {
        return toString().replaceFirst("^w_", "");
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
