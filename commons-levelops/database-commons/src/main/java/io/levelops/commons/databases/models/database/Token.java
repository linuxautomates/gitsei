package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.tokens.TokenData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Token {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "integration_id")
    private String integrationId;

    @JsonProperty(value = "token_data")
    private TokenData tokenData;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}