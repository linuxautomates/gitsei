package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.UserService;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.converters.ModifyUserRequestToUserConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class UsersControllerTest {
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UsersController(userService,
                new ModifyUserRequestToUserConverter(new BCryptPasswordEncoder(), 7779000L))).build();
    }


    @Test
    public void testCreateUser() throws Exception {
        mvc.perform(post("/internal/v1/tenants/test/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"asd@asd.com\",\"first_name\":\"test1\"," +
                        "\"last_name\":\"wompwomp\",\"password\":\"testestestest\"," +
                        "\"password_auth_enabled\":true, \"saml_auth_enabled\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUser() throws Exception {
        when(userService.get("test", "123")).thenReturn(Optional.of(User.builder().id("123").build()));
        mvc.perform(get("/internal/v1/tenants/test/users/123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
