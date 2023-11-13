package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Severity;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestionnaireTestUtils {
    public static Questionnaire buildQuestionnaire(int i, String workItemId, String questionnaireTemplateId, List<UUID> kbIds){
        return buildQuestionnaire(i, workItemId, questionnaireTemplateId, kbIds, Severity.HIGH);
    }

    @SuppressWarnings("rawtypes")
    public static Questionnaire buildQuestionnaire(int i, String workItemId, String questionnaireTemplateId, List<UUID> kbIds, Severity severity){
        String n = String.valueOf(i);
        Questionnaire.QuestionnaireBuilder bldr = Questionnaire.builder()
                .workItemId(workItemId)
                .questionnaireTemplateId(questionnaireTemplateId)
                .senderEmail("sender" + n + "@test.test")
                .targetEmail("target" + n + "@test.test")
                .bucketName("bucketname" +n)
                .bucketPath("bucketpath"+n)
                .answered(0)
                .score(0)
                .priority(severity)
                .totalQuestions(1)
                .totalPossibleScore(1)
                .messageSent(true)
                .state(Questionnaire.State.CREATED)
                .main(true)
                .completedAt(null)
                .createdAt(0L)
                .updatedAt(0L);
        if(CollectionUtils.isNotEmpty(kbIds)) {
            bldr.kbIds(kbIds);
        }
        return bldr.build();
    }

    public static Questionnaire createQuestionnaire(QuestionnaireDBService questionnaireDBService, String company, int i, String workItemId, String questionnaireTemplateId, List<UUID> kbIds, Severity severity) throws SQLException {
        Questionnaire questionnaire = buildQuestionnaire(i, workItemId, questionnaireTemplateId, kbIds, severity);
        String questionnaireId = questionnaireDBService.insert(company, questionnaire);
        Assert.assertNotNull(questionnaireId);
        return questionnaire.toBuilder().id(questionnaireId).build();
    }

    public static List<Questionnaire> createQuestionnaires(QuestionnaireDBService questionnaireDBService, String company, int n, String workItemId, String questionnaireTemplateId, List<UUID> kbIds, Severity severity) throws SQLException {
        List<Questionnaire> result = new ArrayList<>();
        for(int i=0; i < n; i++) {
            result.add(createQuestionnaire(questionnaireDBService, company, i, workItemId, questionnaireTemplateId, kbIds, severity));
        }
        return result;
    }

    public static List<Questionnaire> createQuestionnaires(QuestionnaireDBService questionnaireDBService, String company, String workItemId, List<QuestionnaireTemplate> questionnaireTemplateIds, List<UUID> kbIds, Severity severity) throws SQLException {
        List<Questionnaire> result = new ArrayList<>();
        for(int i=0; i < questionnaireTemplateIds.size(); i++) {
            result.add(createQuestionnaire(questionnaireDBService, company, i, workItemId, questionnaireTemplateIds.get(i).getId(), kbIds, severity));
        }
        return result;
    }

}
