package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.StateService;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class StatesControllerTest {
    private final static String COMPANY = "test";
    private final static String SESSION_USER = "a";
    private MockMvc mvc;

    @Mock
    private StateService stateService;

    private StatesController statesController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        statesController = new StatesController(stateService);
        mvc = MockMvcBuilders
                .standaloneSetup(statesController)
                .build();
    }

    @Test
    public void testCreate() throws Exception {
        ArgumentCaptor<State> argCaptor = ArgumentCaptor.forClass(State.class);
        State expected = State.builder().name("state-name").build();

        doReturn("1").when(stateService).createState(eq(COMPANY), eq(SESSION_USER),argCaptor.capture());
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/states")
                .sessionAttr("session_user", SESSION_USER)
                .sessionAttr("company", COMPANY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(DefaultObjectMapper.get().writeValueAsString(expected)))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        verify(stateService, times(1)).createState(anyString(), anyString(),any());
        Assert.assertEquals(1, argCaptor.getAllValues().size());
        State actual = argCaptor.getValue();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGet() throws Exception {
        State expected = State.builder().id(Integer.parseInt("123")).name("state-name").build();

        doReturn(Optional.of(expected)).when(stateService).getState(eq(COMPANY), eq("123"));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/states/123")
                .sessionAttr("company", COMPANY))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testUpdate() throws Exception {
        ArgumentCaptor<State> argCaptor = ArgumentCaptor.forClass(State.class);
        State expected = State.builder().id(123).name("state-name").build();

        doReturn(true).when(stateService).updateState(eq(COMPANY), eq(SESSION_USER),eq("123"), argCaptor.capture());
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/states/123")
                .sessionAttr("session_user", SESSION_USER)
                .sessionAttr("company", COMPANY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(DefaultObjectMapper.get().writeValueAsString(expected)))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        verify(stateService, times(1)).updateState(anyString(), anyString(), anyString(),any());
        Assert.assertEquals(1, argCaptor.getAllValues().size());
        State actual = argCaptor.getValue();
        Assert.assertEquals(expected, actual);
    }
}