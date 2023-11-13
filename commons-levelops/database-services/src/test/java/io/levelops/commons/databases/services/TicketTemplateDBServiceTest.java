package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.TicketField;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.QuestionnaireTemplateTestUtils.buildQuestionnaireTemplate;
import static io.levelops.commons.databases.services.TicketTemplateUtils.buildTicketTemplate;
import static io.levelops.commons.databases.services.TicketTemplateUtils.modifyTicketTemplate;
import static io.levelops.commons.databases.services.TicketTemplateUtils.verifyTicketTemplate;
import static io.levelops.commons.databases.services.TicketTemplateUtils.verifyTicketTemplatesList;

public class TicketTemplateDBServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private BestPracticesService bestPracticesService;
    private QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private TicketTemplateDBService dbService;
    private String company = "test";
    private List<BestPracticesItem> kbs = null;
    private List<UUID> kbIds = null;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        bestPracticesService = new BestPracticesService(dataSource);
        questionnaireTemplateDBService = new QuestionnaireTemplateDBService(dataSource);
        dbService = new TicketTemplateDBService(dataSource, DefaultObjectMapper.get());
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        bestPracticesService.ensureTableExistence(company);
        questionnaireTemplateDBService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
        kbs = BestPracticesServiceUtils.createBestPracticesItems(bestPracticesService, company, 2);
        kbIds = kbs.stream().map(x -> x.getId()).collect(Collectors.toList());
    }


    @Test
    public void test() throws SQLException {
        Map<String, String> nameTicketTemplateIds = new HashMap<>();
        //Test Create
        QuestionnaireTemplate qt1 = buildQuestionnaireTemplate(1, kbIds);
        String qtId1 = questionnaireTemplateDBService.insert(company, qt1);
        Assert.assertTrue(StringUtils.isNotBlank(qtId1));

        QuestionnaireTemplate qt2 = buildQuestionnaireTemplate(2, kbIds);
        String qtId2 = questionnaireTemplateDBService.insert(company, qt2);
        Assert.assertTrue(StringUtils.isNotBlank(qtId2));

        List<String> questionnaireTemplateIds = Arrays.asList(qtId1, qtId2);

        TicketTemplate tt1 = buildTicketTemplate(1, questionnaireTemplateIds);
        ObjectMapper mapper = new ObjectMapper();
        String data = null;
        try {
            data = mapper.writeValueAsString(tt1);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(data);

        String ticketTemplateId1 = dbService.insert(company, tt1);
        Assert.assertTrue(StringUtils.isNotBlank(ticketTemplateId1));
        nameTicketTemplateIds.put(tt1.getName(), ticketTemplateId1);

        Optional<TicketTemplate> actualTicketTemplateOptional1 = dbService.get(company, ticketTemplateId1);
        Assert.assertNotNull(actualTicketTemplateOptional1);
        Assert.assertTrue(actualTicketTemplateOptional1.isPresent());
        TicketTemplate actualTT1 = actualTicketTemplateOptional1.get();
        Assert.assertEquals(actualTT1.getDescription() , tt1.getDescription());
        verifyTicketTemplate(actualTT1, tt1, ticketTemplateId1);

        DbListResponse<TicketTemplate> dbListResponse1 = dbService.list(company, 0, 50);
        Assert.assertEquals(1, (int) dbListResponse1.getTotalCount());
        Assert.assertEquals(1, dbListResponse1.getRecords().size());
        verifyTicketTemplatesList(dbListResponse1.getRecords(), Collections.singletonList(tt1), nameTicketTemplateIds);

        TicketTemplate tt2 = buildTicketTemplate(2, questionnaireTemplateIds);
        String ticketTemplateId2 = dbService.insert(company, tt2);
        Assert.assertTrue(StringUtils.isNotBlank(ticketTemplateId2));
        nameTicketTemplateIds.put(tt2.getName(), ticketTemplateId2);

        Optional<TicketTemplate> actualTicketTemplateOptional2 = dbService.get(company, ticketTemplateId2);
        Assert.assertNotNull(actualTicketTemplateOptional2);
        Assert.assertTrue(actualTicketTemplateOptional2.isPresent());
        TicketTemplate actualTT2 = actualTicketTemplateOptional2.get();
        Assert.assertEquals(actualTT2.getDescription() , tt2.getDescription());
        verifyTicketTemplate(actualTT2, tt2, ticketTemplateId2);

        DbListResponse<TicketTemplate> dbListResponse2 = dbService.list(company, 0, 50);
        Assert.assertEquals(2, (int) dbListResponse2.getTotalCount());
        Assert.assertEquals(2, dbListResponse2.getRecords().size());

        verifyTicketTemplatesList(dbListResponse2.getRecords(), Arrays.asList(tt1, tt2), nameTicketTemplateIds);

        dbListResponse2 = dbService.listByFilters(company, "tt name 1", 0, 50);
        Assert.assertEquals(1, (int) dbListResponse2.getTotalCount());
        Assert.assertEquals(1, dbListResponse2.getRecords().size());
        verifyTicketTemplatesList(dbListResponse2.getRecords(), Collections.singletonList(tt1), nameTicketTemplateIds);

        boolean deleteSuccess = dbService.delete(company, ticketTemplateId1);
        Assert.assertTrue(deleteSuccess);

        actualTicketTemplateOptional2 = dbService.get(company, ticketTemplateId2);
        Assert.assertNotNull(actualTicketTemplateOptional2);
        Assert.assertTrue(actualTicketTemplateOptional2.isPresent());
        actualTT2 = actualTicketTemplateOptional2.get();
        verifyTicketTemplate(actualTT2, tt2, ticketTemplateId2);

        dbListResponse2 = dbService.list(company, 0, 50);
        Assert.assertEquals(1, (int) dbListResponse2.getTotalCount());
        Assert.assertEquals(1, dbListResponse2.getRecords().size());
        verifyTicketTemplatesList(dbListResponse2.getRecords(), Collections.singletonList(tt2), nameTicketTemplateIds);

        TicketTemplate tt2a = modifyTicketTemplate(3, actualTT2);
        dbService.update(company, tt2a);
        nameTicketTemplateIds.put(tt2a.getName(), ticketTemplateId2);

        actualTicketTemplateOptional2 = dbService.get(company, ticketTemplateId2);
        Assert.assertNotNull(actualTicketTemplateOptional2);
        Assert.assertTrue(actualTicketTemplateOptional2.isPresent());
        actualTT2 = actualTicketTemplateOptional2.get();
        verifyTicketTemplate(actualTT2, tt2a, ticketTemplateId2);

        dbListResponse2 = dbService.list(company, 0, 50);
        Assert.assertEquals(1, (int) dbListResponse2.getTotalCount());
        Assert.assertEquals(1, dbListResponse2.getRecords().size());
        verifyTicketTemplatesList(dbListResponse2.getRecords(), Collections.singletonList(tt2a), nameTicketTemplateIds);

        //Test null options
        TicketTemplate tt3 = TicketTemplateUtils.buildTicketTemplate(3, questionnaireTemplateIds, true);
        String ticketTemplateId3 = dbService.insert(company, tt3);
        Assert.assertTrue(StringUtils.isNotBlank(ticketTemplateId3));
    }

    @Test
    public void testUpdateAndDelete() throws SQLException {
        //Test Create
        QuestionnaireTemplate qt1 = buildQuestionnaireTemplate(1, kbIds);
        String qtId1 = questionnaireTemplateDBService.insert(company, qt1);
        Assert.assertTrue(StringUtils.isNotBlank(qtId1));

        QuestionnaireTemplate qt2 = buildQuestionnaireTemplate(2, kbIds);
        String qtId2 = questionnaireTemplateDBService.insert(company, qt2);
        Assert.assertTrue(StringUtils.isNotBlank(qtId2));

        List<String> questionnaireTemplateIds = Arrays.asList(qtId1, qtId2);

        TicketTemplate tt1 = buildTicketTemplate(1, questionnaireTemplateIds);
        String ticketTemplateId1 = dbService.insert(company, tt1);
        Assert.assertTrue(StringUtils.isNotBlank(ticketTemplateId1));

        Optional<TicketTemplate> actualTicketTemplateOptional1 = dbService.get(company, ticketTemplateId1);
        Assert.assertNotNull(actualTicketTemplateOptional1);
        Assert.assertTrue(actualTicketTemplateOptional1.isPresent());
        TicketTemplate actualTT1 = actualTicketTemplateOptional1.get();
        verifyTicketTemplate(actualTT1, tt1, ticketTemplateId1);

        List<TicketField> updatedFields = new ArrayList<>();
        for (int i = 0; i < actualTT1.getTicketFields().size(); i++) {
            if (i == 0) {
                updatedFields.add(actualTT1.getTicketFields().get(i));
            } else {
                updatedFields.add(actualTT1.getTicketFields().get(i).toBuilder().deleted(true).build());
            }
        }

        int index = actualTT1.getTicketFields().size() + 1;
        KvField.KvFieldBuilder bldr = KvField.builder()
                .key("k" + index).type("t" + index).required(true).dynamicResourceName("d" + index).validation("v" + index).searchField("s" + index).displayName("dn" + index).description("desc" + index);
        updatedFields.add(TicketField.builder().field(bldr.build()).build());

        TicketTemplate modifiedTT1 = actualTT1.toBuilder().description("updated test issue").clearTicketFields().ticketFields(updatedFields).build();
        boolean success = dbService.update(company, modifiedTT1);
        Assert.assertTrue(success);

        actualTicketTemplateOptional1 = dbService.get(company, ticketTemplateId1);
        Assert.assertNotNull(actualTicketTemplateOptional1);
        Assert.assertTrue(actualTicketTemplateOptional1.isPresent());
        TicketTemplate actualTT1Updated = actualTicketTemplateOptional1.get();
        Assert.assertEquals(2, actualTT1Updated.getTicketFields().size());
        Assert.assertEquals("updated test issue", actualTT1Updated.getDescription());
    }

    @Test
    public void testBulkDelete() throws SQLException {
        QuestionnaireTemplate qt1 = buildQuestionnaireTemplate(1, kbIds);
        String qtId1 = questionnaireTemplateDBService.insert(company, qt1);
        QuestionnaireTemplate qt2 = buildQuestionnaireTemplate(2, kbIds);
        String qtId2 = questionnaireTemplateDBService.insert(company, qt2);
        List<String> questionnaireTemplateIds1 = Arrays.asList(qtId1, qtId2);
        TicketTemplate tt1 = buildTicketTemplate(1, questionnaireTemplateIds1);
        QuestionnaireTemplate qt3 = buildQuestionnaireTemplate(3, kbIds);
        String qtId3 = questionnaireTemplateDBService.insert(company, qt3);
        QuestionnaireTemplate qt4 = buildQuestionnaireTemplate(4, kbIds);
        String qtId4 = questionnaireTemplateDBService.insert(company, qt4);
        List<String> questionnaireTemplateIds2 = Arrays.asList(qtId3, qtId4);
        TicketTemplate tt2 = buildTicketTemplate(2, questionnaireTemplateIds2);
        String ticketTemplateId1 = dbService.insert(company, tt1);
        String ticketTemplateId2 = dbService.insert(company, tt2);
        dbService.bulkDelete(company, List.of(ticketTemplateId1, ticketTemplateId2));
        Assertions.assertThat(dbService.get(company, ticketTemplateId1)).isEmpty();
        Assertions.assertThat(dbService.get(company, ticketTemplateId2)).isEmpty();
    }
}