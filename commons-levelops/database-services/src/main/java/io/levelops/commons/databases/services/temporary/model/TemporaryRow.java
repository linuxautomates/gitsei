package io.levelops.commons.databases.services.temporary.model;

import lombok.Builder;
import lombok.Data;

import java.util.regex.Pattern;

@Data
public class TemporaryRow {
    private static Pattern UNSAFE_PG_PATTERN = Pattern.compile("\\\\+u0000");

    private String id;
    private String levelopsDataField;
    private Long updatedAt;

    @Builder
    public TemporaryRow(String id, String levelopsDataField, Long updatedAt, Boolean sanitize) {
        this.id = id;
        //this check means that null and True is treated as True
        if (Boolean.FALSE.equals(sanitize))
            this.levelopsDataField = levelopsDataField;
        else
            this.levelopsDataField = UNSAFE_PG_PATTERN.matcher(levelopsDataField).replaceAll("");
        this.updatedAt = updatedAt;
    }

}
