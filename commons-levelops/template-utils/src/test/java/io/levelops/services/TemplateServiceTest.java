package io.levelops.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class TemplateServiceTest {

    private TemplateService templateService;

    @Before
    public void setUp() throws Exception {
        templateService = new TemplateService();
    }

    @Test
    public void testString() {

        String out = templateService.evaluateTemplate("Hello, ${var}!", Map.of("var", "world"));
        assertThat(out).isEqualTo("Hello, world!");
    }

    @Test
    public void testString2() {

        String out = templateService.evaluateTemplate("Please fill out questionnaire $link being sent to you by $sender\n$info",
                Map.of("link", "https://link", "sender", "a.b@c.com", "info", "wassup"));
        assertThat(out).isEqualTo("Please fill out questionnaire https://link being sent to you by a.b@c.com\n" +
                "wassup");
    }


//    @Test
//    public void testFile() {
//        TemplateService templateService = new TemplateService();
//        String out = templateService.evaluateTemplateFromResource("template.vm", Map.of("var", "world"));
//        assertThat(out).isEqualTo("Hello, world!");
//    }
}