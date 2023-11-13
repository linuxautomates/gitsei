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
public class ForgotPassRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6423628838793202976L;
    @JsonProperty("company")
    private String company;
    @JsonProperty("username")
    private String username;
}