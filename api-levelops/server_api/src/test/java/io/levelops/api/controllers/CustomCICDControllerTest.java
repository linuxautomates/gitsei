package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.commons.generic.clients.GenericRequestsClient;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class CustomCICDControllerTest {
    @Autowired
    private GenericRequestsClient client;

    private MockMvc mvc;
    private CustomCICDController controller;
    private ObjectMapper mapper = DefaultObjectMapper.get();

    @Before
    public void setup() throws IOException {
        controller = new CustomCICDController(client, mapper);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testJobNameFieldForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("pipeline");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_name/pipeline field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJobNameFieldForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("job_name");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_name/pipeline field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInstanceNameFieldForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("instance_name");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("jenkins_instance_name/instance_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInstanceNameFieldForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("jenkins_instance_name");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("jenkins_instance_name/instance_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInstanceGUIDFieldForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("instance_guid");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("jenkins_instance_guid/instance_guid field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInstanceGUIDFieldForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("jenkins_instance_guid");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("jenkins_instance_guid/instance_guid field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJobNormalizedFullNameForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("qualified_name");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_normalized_full_name/qualified_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJobNormalizedFullNameForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("job_normalized_full_name");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_normalized_full_name/qualified_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJobFullNameFieldForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.put("job_full_name", null);
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_full_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJobFullNameFieldForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.put("job_full_name", null);
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("job_full_name field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testResultFieldForCustomCICD() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/custom_cicd_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("result");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("result field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testResultFieldForJenkins() throws Exception {
        String data = ResourceUtils.getResourceAsString("model/jenkins_generic_request_payload.json");
        Map<String, Object> payload = mapper.readValue(data, Map.class);

        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().isAccepted());

        payload.remove("result");
        mvc.perform(asyncDispatch(mvc.perform(MockMvcRequestBuilders.post("/v1/custom-cicd")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                        .andExpect(result -> {
                            Assert.assertEquals("result field is missing in request", ((GenericResponse)((ResponseEntity) result.getAsyncResult()).getBody()).getPayload());
                            Assert.assertEquals(HttpStatus.BAD_REQUEST, ((ResponseEntity) result.getAsyncResult()).getStatusCode());
                        })
                        .andReturn()))
                .andExpect(status().isBadRequest());
    }
}
