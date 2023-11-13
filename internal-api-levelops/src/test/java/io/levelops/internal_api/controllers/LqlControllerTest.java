package io.levelops.internal_api.controllers;

import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class LqlControllerTest {
    private MockMvc mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new LqlController()).build();
    }

    @Test
    public void testCreateToken() throws Exception {
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/lql/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lqls\": [\"code.type = stuff\"]}"))
                .andReturn()))
                .andExpect(status().isOk());
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/lql/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lqls\": [\"code.type contains stuff\"]}"))
                .andReturn()))
                .andExpect(status().isBadRequest());
    }

}
