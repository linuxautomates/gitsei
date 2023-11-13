package io.levelops.auth.auth.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Component("auth")
public final class Auth {
    @Value("${LEGACY_REQUEST:true}")
    private boolean legacy;
}
