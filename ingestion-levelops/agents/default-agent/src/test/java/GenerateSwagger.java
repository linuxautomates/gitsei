import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class GenerateSwagger {
//
//    @Autowired
//    WebApplicationContext context;
//
//    @Test
//    public void generateSwagger() throws Exception {
//        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
//        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs").accept(MediaType.APPLICATION_JSON))
//                .andDo((result) -> {
//                    FileUtils.writeStringToFile(new File("swagger.json"), result.getResponse().getContentAsString());
//                });
//
//    }
}