package io.levelops.auth.httpmodels;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class ChangePassRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -5498201919703079917L;
    @JsonProperty("token")
    private String token;
    @JsonProperty("new_password")
    private String newPassword;
}