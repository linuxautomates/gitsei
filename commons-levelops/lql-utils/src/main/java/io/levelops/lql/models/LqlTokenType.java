package io.levelops.lql.models;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum LqlTokenType {
    LPAREN,     // (
    RPAREN,     // )
    LBRACKET,   // [
    RBRACKET,   // ]
    COMMA,      // ,
    IDENTIFIER, // variable
    OPERATOR,   // =, !=, ...
    KEYWORD,    // and, or, not, is, null, empty
    LITERAL;    // 6.02e23, "music"

    @JsonValue
    public String toString() {
        return super.toString();
    }

    @JsonValue
    @Nullable
    public LqlTokenType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(LqlTokenType.class, value);
    }
}