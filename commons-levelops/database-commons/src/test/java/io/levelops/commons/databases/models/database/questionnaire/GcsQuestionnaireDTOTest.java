package io.levelops.commons.databases.models.database.questionnaire;

import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GcsQuestionnaireDTOTest {

    @Test
    public void testDeSerialization() throws IOException {
        GcsQuestionnaireDTO q = ResourceUtils.getResourceAsObject("questionnaire_gcs/questionnaire_gcs.json", GcsQuestionnaireDTO.class);
        Assert.assertNotNull(q);
        Assert.assertEquals(q.getAssignmentMsg(), "Please fill out questionnaire new test template available at https://testui1.levelops.io/#/admin/answer-questionnaire-page?questionnaire=eff9bd12-a121-4dfd-9008-766f91199ae0&tenant=foo being sent to you by meghana@levelops.io\n\n\n");
        Assert.assertEquals(3, q.getSections().size());

        Section s = q.getSections().get(0);
        Assert.assertTrue(CollectionUtils.isEmpty(s.getTags()));
        Assert.assertEquals("3281de37-7901-4f9d-8488-d1387ec1006d", s.getId());
        Assert.assertEquals("some new question", s.getName());
        Assert.assertEquals(Section.Type.DEFAULT, s.getType());
        Assert.assertEquals("some desc", s.getDescription());
        Assert.assertEquals(5, s.getQuestions().size());

        Question q1 = s.getQuestions().get(0);
        Assert.assertEquals("eb9b9412-f96b-433c-875e-087a47b65ac8", q1.getId());
        Assert.assertEquals("ass 1", q1.getName());
        Assert.assertEquals(Severity.MEDIUM, q1.getSeverity());
        Assert.assertEquals("3281de37-7901-4f9d-8488-d1387ec1006d", q1.getSectionId());
        Assert.assertEquals("multi-select", q1.getType());
        Assert.assertEquals(2, q1.getOptions().size());
        Assert.assertEquals("option 1", q1.getOptions().get(0).getResponse());
        Assert.assertEquals(1, q1.getOptions().get(0).getScore().intValue());
        Assert.assertEquals("option 2", q1.getOptions().get(1).getResponse());
        Assert.assertEquals(1, q1.getOptions().get(1).getScore().intValue());

        Assert.assertEquals(3, q.getSectionResponses().size());
        SectionResponse sr = q.getSectionResponses().get(0);
        Assert.assertEquals("3281de37-7901-4f9d-8488-d1387ec1006d", sr.getSectionId());
        Assert.assertEquals(5, sr.getAnswers().size());

        Answer a = sr.getAnswers().get(0);
        Assert.assertEquals("eb9b9412-f96b-433c-875e-087a47b65ac8", a.getQuestionId());
        Assert.assertEquals("meghana@levelops.io", a.getUserEmail());
        Assert.assertEquals(true, a.isAnswered());
        Assert.assertEquals(false, a.isUpload());
        Assert.assertEquals(1, a.getResponses().size());
        Assert.assertEquals("option 1", a.getResponses().get(0).getValue());
        Assert.assertEquals(1, a.getResponses().get(0).getScore().intValue());
    }
}