package io.levelops.internal_api.converters;

import io.levelops.users.requests.ModifyUserRequest;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

public class ModifyUserRequestToUserConverterTest {
    private ModifyUserRequestToUserConverter modifyUserRequestToUserConverter;

    
    @Test
    public void test(){
        modifyUserRequestToUserConverter = new ModifyUserRequestToUserConverter(new StandardPasswordEncoder("test"), 200L);
        var source = new ModifyUserRequest("", "name", "lastname", "usertype", false, true, false, false, null, null, null, null);
        var result = modifyUserRequestToUserConverter.convertNewUserRequest(source);
        
        Assertions.assertThat(result).isNotNull();

        var result2 = modifyUserRequestToUserConverter.convertUpdateRequest(source, "id");
        
        Assertions.assertThat(result2).isNotNull();
    }
}
