package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestionnaireTemplateTestUtils {
    public static QuestionnaireTemplate buildQuestionnaireTemplate(int i, List<UUID> kbIds){
        return QuestionnaireTemplate.builder()
                .name("qt name " + String.valueOf(i)).lowRiskBoundary(300).midRiskBoundary(500).kbIds(kbIds).build();
    }

    public static QuestionnaireTemplate createQuestionnaireTemplate(QuestionnaireTemplateDBService questionnaireTemplateDBService, String company, int i, List<UUID> kbIds) throws SQLException {
        QuestionnaireTemplate qt = buildQuestionnaireTemplate(i, kbIds);
        String id = questionnaireTemplateDBService.insert(company, qt);
        Assert.assertNotNull(id);
        return qt.toBuilder().id(id).build();
    }

    public static List<QuestionnaireTemplate> createQuestionnaireTemplates(QuestionnaireTemplateDBService questionnaireTemplateDBService, String company, int n, List<UUID> kbIds) throws SQLException {
        List<QuestionnaireTemplate> questionnaireTemplates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            questionnaireTemplates.add(createQuestionnaireTemplate(questionnaireTemplateDBService, company, i, kbIds));
        }
        return questionnaireTemplates;
    }
}
