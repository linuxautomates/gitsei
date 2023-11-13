package io.levelops.auth.config;

import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class EntitlementAPIMappingConfigTest {

    @Test
    public void test(){
        var m = new RequestMappingHandlerMapping();
        var service = new EntitlementAPIMappingConfig(m);
    }
    
}
