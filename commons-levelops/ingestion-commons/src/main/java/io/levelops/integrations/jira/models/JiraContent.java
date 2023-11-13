package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraContent.JiraContentBuilder.class)
public class JiraContent {
    @JsonProperty("type")
    String type;

    @JsonProperty("text")
    String text;

    @JsonProperty("content")
    List<JiraContent> entries;

    @Nonnull
    @SuppressWarnings("rawtypes")
    public static String toString(@Nullable Object content) {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Map) {
            return generateDescriptionText((Map) content);
        }
        if (content instanceof JiraContent) {
            return generateDescriptionText((JiraContent) content);
        }
        return "";
    }

    /**
     * Needed for keeping models compatible with both v2 and v3.
     * Otherwise we can use JiraIssueDescription.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String generateDescriptionText(@Nullable Map content) {
        if (content == null) {
            return "";
        }
        String text = StringUtils.defaultString((String) content.get("text"));
        Collection items = CollectionUtils.emptyIfNull((List) content.get("content"));
        return (String) Stream.concat(Stream.of(text), items.stream()
                .map(m -> JiraContent.generateDescriptionText((Map) m)))
                .filter(s -> StringUtils.isNotEmpty((String) s))
                .collect(Collectors.joining("\n"));
    }

    public static String generateDescriptionText(@Nullable JiraContent content) {
        if (content == null) {
            return "";
        }
        String text = StringUtils.defaultString(content.getText());
        Collection<JiraContent> items = CollectionUtils.emptyIfNull(content.getEntries());
        return Stream.concat(Stream.of(text), items.stream()
                .map(JiraContent::generateDescriptionText))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining("\n"));
    }
}
