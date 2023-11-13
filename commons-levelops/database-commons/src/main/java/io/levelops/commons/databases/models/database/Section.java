package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Section {
    //this has tags but thats for the individual controllers to handle.
    private List<String> tags;

    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private Type type;

    @JsonProperty(value = "attachment")
    private String attachment;

    @JsonProperty(value = "description")
    private String description;

    @Singular
    @JsonProperty(value = "questions")
    @JsonAlias(value = "assertions")
    private List<Question> questions;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    public enum Type {
        CHECKLIST,
        DEFAULT;

        @JsonCreator
        @Nullable
        public static Type fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Type.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}