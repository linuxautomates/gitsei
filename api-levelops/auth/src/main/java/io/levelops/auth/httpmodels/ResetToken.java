package io.levelops.auth.httpmodels;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ResetToken {
    @JsonProperty(value = "token")
    private String token;
    @JsonProperty(value = "username")
    private String username;
    @JsonProperty(value = "company")
    private String company;
}