package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubUser.GithubUserBuilder.class)
public class GithubUser implements Serializable {

    @JsonProperty("type")
    OwnerType type;

    @JsonProperty("login")
    String login;

    @Nullable
    @JsonProperty("orgVerifiedDomainEmails")
    List<String> orgVerifiedDomainEmails;

    @Nullable
    @JsonProperty("name")
    String name;

    public enum OwnerType implements Serializable {
        USER,
        ORGANIZATION;

        @JsonCreator
        public static OwnerType fromString(String value) {
            return EnumUtils.getEnumIgnoreCase(OwnerType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
