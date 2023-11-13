package io.levelops.auth.auth.authobject;

import org.junit.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Set;

public class ExtendedUserTest {
    
    @Test
    public void test(){
        var user = ExtendedUser.ExtendedUserBuilder()
            .username("u")
            .password("p")
            .authorities(Set.of(new SimpleGrantedAuthority("s")))
            .build();
    }
}
