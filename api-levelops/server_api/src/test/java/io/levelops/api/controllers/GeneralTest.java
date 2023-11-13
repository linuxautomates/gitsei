package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireListItemDTO;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;
import static org.junit.Assert.assertEquals;

// @Log4j2
public class GeneralTest {

    // @Test
    // public void templateTest(){
    //     TemplateService templateService = new TemplateService();
    //     var text = templateService.evaluateTemplate("test $baseUrl", Map.of("baseUrl", "https://test.test"));
    //     log.info(text);
    // }

    @Test
    public void test() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        System.out.println("Test");
        System.out.println("" + objectMapper.readValue("{"
            + "\"name\": \"question1\","
            + "\"tags\": ["
            + "    \"tag1\""
            + "],"
            + " \"description\": \"question1\","
            + " \"assertions\": ["
            + "     {"
            + "         \"name\": \"assertion1\","
            + "         \"type\": \"multi-select\","
            + "         \"options\": ["
            + "             {"
            + "                 \"value\": \"option1\","
            + "                \"score\": 10"
            + "            },"
            + "            {"
            + "                \"value\": \"option2\","
            + "                \"score\": 10"
            + "            }"
            + "        ],"
            + "        \"custom\": true,"
            + "        \"verifiable\": true,"
            + "        \"verification_mode\": \"auto\","
            + "        \"verification_assets\": [],"
            + "        \"training\": []"
            + "    }"
            + "]"
            + "}", Question.class));
        
        QuestionnaireTemplate q = QuestionnaireTemplate.builder()
        .id("112")
        .sections(Collections.singletonList(UUID.randomUUID()))
        .build();
        String d = objectMapper.writeValueAsString(q);
        System.out.println("JSON:   " + d);

        var value = new String(ClassLoader.getSystemResourceAsStream("questionnaireDTO.json").readAllBytes(), Charset.forName("utf-8"));
        System.out.println("json: " + value);
        System.out.println("qDTO:  " + objectMapper.readValue(value, QuestionnaireDTO.class));


        System.out.println("T:   " + objectMapper.writeValueAsString(QuestionnaireListItemDTO.builder().currentScore(2).build()));

        System.out.println("qqq ::   " + objectMapper.writeValueAsString( QuestionnaireDTO.builder()
        .notificationMessage("")
        .notificationTemplateId("id")
        .build()) );

        value = new String(ClassLoader.getSystemResourceAsStream("questionnaire_update.json").readAllBytes(), Charset.forName("utf-8"));
        var questionnaire = objectMapper.readValue(value, QuestionnaireDTO.class);
        System.out.println("Update" + objectMapper.readValue(value, QuestionnaireDTO.class));
        var score = QuestionnaireDTO.calculateScore(questionnaire.getAnswers());
        System.out.println("Score: " + score);

        assertEquals(57, score);
    }
}