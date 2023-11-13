package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.ActionMode;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Section.Type;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

public class SectionsServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;

    private TagsService tagsService;
    private TagItemDBService tagItemDBService;
    private SectionsService sectionsService;
    private BestPracticesService bestPracticesService;
    private QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private String company = "test";

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        tagsService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        sectionsService = new SectionsService(dataSource, DefaultObjectMapper.get(), tagItemDBService);
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);

        new DatabaseSchemaService(dataSource)
            .ensureSchemaExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        sectionsService.ensureTableExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
    }

    private static final String QUESTION_TAG_NAME_PREFIX = "question-tag-name";

    private List<Question.Option> createQuestionOptions(){
        Question.Option op1 = new Question.Option("YES", 5, true);
        Question.Option op2 = new Question.Option("NO", 1, false);
        return Arrays.asList(op1, op2);
    }

    private List<String> createTags(String tagNamePrefix) throws SQLException {
        List<String> questionTagIds = new ArrayList<>();
        for (int i=0; i<2; i++){
            Tag t = Tag.builder().name(tagNamePrefix +i).build();
            String tagId = tagsService.insert(company,t);
            questionTagIds.add(tagId);
        }
        return questionTagIds;
    }
    private Question createQuestion(int n, List<Question.Option> ops, List<String> tagIds){
        Severity sev = ((n%2)==0)?Severity.HIGH:Severity.LOW;
        Question.QuestionBuilder bldr = Question.builder()
                .id(UUID.randomUUID().toString())
                .name("q" +n).severity(sev).type("boolean").options(ops).verificationMode(ActionMode.AUTO).number(n+1).required(true);
        if(CollectionUtils.isNotEmpty(tagIds)){
            bldr.tagIds(tagIds);
        }
        return bldr.build();
    }
    private List<Question> createQuestions(int n, List<Question.Option> ops, List<String> tagIds){
        List<Question> r = new ArrayList<>();
        for(int i=0; i< n; i++){
            r.add(createQuestion(n,ops, ((i%2)==0)?tagIds: null));
        }
        return r;
    }
    private Section createSection(int n,List<Question.Option> ops, List<String> questionTagIds){
        //return Section.builder().id(id).name("section" + (n+1)).type(Type.DEFAULT).description("section" + (n+1)).questions(createQuestions(numOfQuestions, id, ops, questionTagIds)).build();
        return Section.builder()
                .name("section" + (n+1))
                .type(Type.DEFAULT)
                .description("section" + (n+1))
                .questions(createQuestions(n, ops, questionTagIds))
                .build();
    }

    private Map<String,Question> questionListToMap(List<Question> q) {
        return q.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
    }
    private Map<String,Section> sectionListToMap(List<Section> s) {
        return s.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
    }

    private void verifyQuestion(Question e, Question a){
        Assert.assertEquals(e.getId(),a.getId());
        Assert.assertEquals(e.getName(),a.getName());
        Assert.assertEquals(e.getSeverity(),a.getSeverity());
        Assert.assertEquals(e.getSectionId(),a.getSectionId());
        Assert.assertEquals(e.getType(),a.getType());
        if(CollectionUtils.isNotEmpty(e.getOptions())) {
            Assert.assertEquals(e.getOptions().stream().collect(Collectors.toSet()), a.getOptions().stream().collect(Collectors.toSet()));
        }
        Assert.assertEquals(e.getVerifiable(),a.getVerifiable());
        Assert.assertEquals(e.getVerificationMode(),a.getVerificationMode());
        Assert.assertEquals(e.getNumber(),a.getNumber());
        Assert.assertEquals(e.getRequired(),a.getRequired());
        if(CollectionUtils.isNotEmpty(e.getTagIds())) {
            Assert.assertEquals(e.getTagIds().stream().collect(Collectors.toSet()), a.getTagIds().stream().collect(Collectors.toSet()));
        }
    }
    private void verifyQuestions(List<Question> e, List<Question> a){
        Assert.assertEquals(e.size(), a.size());
        Map<String,Question> em = questionListToMap(e);
        Map<String,Question> am = questionListToMap(a);
        for(String id : em.keySet()){
            verifyQuestion(em.get(id),am.get(id));
        }
    }
    private void verifySection(Section e, Section a){
        Assert.assertEquals(e.getId(), a.getId());
        verifyQuestions(e.getQuestions(), a.getQuestions());
    }
    private void verifySections(List<Section> e, List<Section> a){
        Assert.assertEquals(e.size(), a.size());
        Map<String,Section> em = sectionListToMap(e);
        Map<String,Section> am = sectionListToMap(a);
        for(String id : em.keySet()){
            verifySection(em.get(id),am.get(id));
        }
    }

    private List<Section> testCreateSections(int n,List<Question.Option> ops, List<String> questionTagIds) throws SQLException {
        List<Section> insertedSections = new ArrayList<>();
        for(int i=0; i<n; i++){
            Section section = createSection(i,ops,questionTagIds);
            String sectionId = sectionsService.insert(company, section);
            Assert.assertNotNull(sectionId);
            List<Question> questions = section.getQuestions().stream()
                    .map(q -> q.toBuilder().sectionId(sectionId).build())
                    .collect(Collectors.toList());
            Section insertedSection = section.toBuilder().id(sectionId).clearQuestions().questions(questions).build();
            insertedSections.add(insertedSection);
        }
        return insertedSections;
    }

    @Test
    public void test() throws SQLException {
        List<String> questionTagIds = createTags(QUESTION_TAG_NAME_PREFIX);
        List<Question.Option> ops = createQuestionOptions();
        List<Section> sections = testCreateSections(4, ops, questionTagIds);
        for (Section section : sections){
            Optional<Section> optionalSection = sectionsService.get(company, section.getId());
            Assert.assertNotNull(optionalSection);
            Assert.assertTrue(optionalSection.isPresent());
            Section actual = optionalSection.get();
            verifySection(section, actual);
        }
        List<Section> sectionsWithQuestions = sections.stream().filter(s -> CollectionUtils.isNotEmpty(s.getQuestions())).collect(Collectors.toList());
        for(String questionTagId : questionTagIds){
            DbListResponse<Section> dbListResponse = sectionsService.listByTagIds(company, null,null,0,500,Arrays.asList(questionTagId));
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(sectionsWithQuestions.size(), dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(sectionsWithQuestions.size(), dbListResponse.getRecords().size());
            verifySections(sectionsWithQuestions, dbListResponse.getRecords());
        }
        DbListResponse<Section> response = sectionsService.list(company, 0, 10);
        Assert.assertEquals(Integer.valueOf(4), response.getCount());
        Set<String> responseIds = response.getRecords().stream().map(q -> q.getId()).collect(Collectors.toSet());
        Set<String> expectedIds = sections.stream().map(q -> q.getId()).collect(Collectors.toSet());
        Assert.assertEquals(expectedIds, responseIds);
        String id = expectedIds.iterator().next();
        Assert.assertFalse(sectionsService.checkIfUsedInQuestionnaireTemplates(company, id).isPresent());
        QuestionnaireTemplate qTemplate = QuestionnaireTemplate.builder()
            .name("QTemplate 1")
            .sections(expectedIds.stream().map(UUID::fromString).collect(Collectors.toList()))
            .createdAt(Instant.now().toEpochMilli())
            .lowRiskBoundary(1)
            .midRiskBoundary(2)
            .build();
        String templateId = questionnaireTemplateDBService.insert(company, qTemplate);
        Assert.assertEquals(true, sectionsService.checkIfUsedInQuestionnaireTemplates(company, id).isPresent());
        questionnaireTemplateDBService.deleteAndReturn(company, templateId);
        Assert.assertFalse(sectionsService.checkIfUsedInQuestionnaireTemplates(company, id).isPresent());

        Assert.assertTrue(sectionsService.delete(company,id));

    }
}