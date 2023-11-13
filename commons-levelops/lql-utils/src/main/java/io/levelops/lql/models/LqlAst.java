package io.levelops.lql.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonDeserialize(builder = LqlAst.LqlAstBuilder.class)
@Log4j2
public class LqlAst {

    @JsonProperty("token")
    private LqlToken token;

    @Singular
    @JsonProperty("children")
    private final List<LqlAst> children;

    /**
     * Store temporary value during evaluation
     */
    @JsonIgnore
    private Boolean eval;

    /**
     * Mutate token. USE CAREFULLY! This can mutate the AST and render it invalid.
     */
    public LqlAst setToken(LqlToken token) {
        this.token = token;
        return this;
    }

    public LqlAst setEval(Boolean eval) {
        this.eval = eval;
        return this;
    }

    public static LqlAst leaf(LqlToken token) {
        return LqlAst.builder()
                .token(token)
                .build();
    }

    public static LqlAst operator(LqlToken operator, LqlToken... args) {
        LqlAstBuilder builder = LqlAst.builder()
                .token(operator);
        for (LqlToken arg : args) {
            builder.child(LqlAst.leaf(arg));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "<" + token.getValue() + ">"
                + (children.isEmpty() ? "" : String.join(", ", children.toString()))
                + (eval == null ? "" : " (" + eval + ")");
    }

    public String toPrettyString() {
        return toPrettyString("   ", 0);
    }

    private String toPrettyString(String indentationString, int indentationLevel) {
        String indent = StringUtils.repeat(indentationString, indentationLevel);
        StringBuilder builder = new StringBuilder();
        builder.append(indent).append(indentationLevel % 2 == 0 ? "► " : "▷ ")
                .append(token.getValue())
                .append("\n");
        for (LqlAst child : children) {
            builder.append(child.toPrettyString(indentationString, indentationLevel + 1));
        }
        return builder.toString();
    }

    public String toInlineString() {
        if (children.isEmpty()) {
            return token.getRawValue();
        } else if (children.size() == 1) {
            // unary operator like "not"
            return "(" + token.getRawValue() + " " + children.get(0).toInlineString() + ")";
        } else {
            return "(" +
                    children.get(0).toInlineString() + " " + token.getRawValue() + " " +
                    children.subList(1, children.size()).stream()
                            .map(LqlAst::toInlineString)
                            .collect(Collectors.joining(" ")) +
                    ")";
        }
    }
}
