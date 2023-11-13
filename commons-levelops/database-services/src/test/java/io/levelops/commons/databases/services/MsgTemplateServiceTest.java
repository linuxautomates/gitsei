package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.MessageTemplate.TemplateType;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MsgTemplateServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private MsgTemplateService messageTemplateService;

    private String company = "test";

    @Before
    public void setup() throws SQLException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        this.messageTemplateService = new MsgTemplateService(dataSource);
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA " + company).execute();
        this.messageTemplateService.ensureTableExistence(company);
    }

    @Test
    public void testInsertDefaultEmail() throws SQLException {
        var template = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.EMAIL)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t1")
            .system(true)
            .botName("")
            .build();
        var id = messageTemplateService.insert(company, template);
        Assertions.assertThat(id).isNotBlank();

        var template3 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.SLACK)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t3")
            .system(true)
            .botName("b3")
            .build();
        var id3 = messageTemplateService.insert(company, template3);

        var template2 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.EMAIL)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t2")
            .system(true)
            .botName("")
            .build();
        var id2 = messageTemplateService.insert(company, template2);
        Assertions.assertThat(id2).isNotBlank();

        var db1 = messageTemplateService.get(company, id);
        var db2 = messageTemplateService.get(company, id2);
        var db3 = messageTemplateService.get(company, id3);

        Assertions.assertThat(db1.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db2.get().isDefaultTemplate()).isTrue();
        Assertions.assertThat(db3.get().isDefaultTemplate()).isTrue();
    }

    @Test
    public void testInsertDefaultSlack() throws SQLException {
        var template = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.SLACK)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t1")
            .system(true)
            .botName("b1")
            .build();
        var id = messageTemplateService.insert(company, template);
        Assertions.assertThat(id).isNotBlank();

        var template3 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.EMAIL)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t3")
            .system(true)
            .botName("")
            .build();
        var id3 = messageTemplateService.insert(company, template3);

        var template2 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.SLACK)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t2")
            .system(true)
            .botName("b2")
            .build();
        var id2 = messageTemplateService.insert(company, template2);
        Assertions.assertThat(id2).isNotBlank();

        var db1 = messageTemplateService.get(company, id);
        var db2 = messageTemplateService.get(company, id2);
        var db3 = messageTemplateService.get(company, id3);

        Assertions.assertThat(db1.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db2.get().isDefaultTemplate()).isTrue();
        Assertions.assertThat(db3.get().isDefaultTemplate()).isTrue();
    }

    private void verifyRecord(MessageTemplate a, MessageTemplate e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getType(), e.getType());
        Assert.assertEquals(a.getBotName(), e.getBotName());
        Assert.assertEquals(a.getEmailSubject(), e.getEmailSubject());
        Assert.assertEquals(a.getMessage(), e.getMessage());
        Assert.assertEquals(a.isDefaultTemplate(), e.isDefaultTemplate());
        Assert.assertEquals(a.isSystem(), e.isSystem());
        Assert.assertEquals(a.getEventType(), e.getEventType());
        Assert.assertNotNull(a.getCreatedAt());
    }

    private void verifyRecords(List<MessageTemplate> a, List<MessageTemplate> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, MessageTemplate> actualMap = a.stream().collect(Collectors.toMap(MessageTemplate::getId, x -> x));
        Map<String, MessageTemplate> expectedMap = e.stream().collect(Collectors.toMap(MessageTemplate::getId, x -> x));

        for (String key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    @Test
    public void testUpdateDefault() throws SQLException {
        var template = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.EMAIL)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t1")
            .system(true)
            .botName("")
            .build();
        var id = messageTemplateService.insert(company, template);
        Assertions.assertThat(id).isNotBlank();
        template = template.toBuilder().id(id).build();

        var template2 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.EMAIL)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_CREATED)
            .message("test")
            .name("t2")
            .system(true)
            .botName("")
            .build();
        var id2 = messageTemplateService.insert(company, template2);
        Assertions.assertThat(id2).isNotBlank();
        template2 = template2.toBuilder().id(id2).build();

        var template3 = MessageTemplate.builder()
            .defaultTemplate(true)
            .type(TemplateType.SLACK)
            .emailSubject("me")
            .eventType(EventType.ASSESSMENT_NOTIFIED)
            .message("test")
            .name("t3")
            .system(true)
            .botName("b3")
            .build();
        var id3 = messageTemplateService.insert(company, template3);
        template3 = template3.toBuilder().id(id3).build();

        var template4 = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.SLACK)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_NOTIFIED)
                .message("test")
                .name("t4")
                .system(true)
                .botName("b3")
                .build();
        var id4 = messageTemplateService.insert(company, template4);
        template4 = template4.toBuilder().id(id4).build();

        var db1 = messageTemplateService.get(company, id);
        var db2 = messageTemplateService.get(company, id2);
        var db3 = messageTemplateService.get(company, id3);
        var db4 = messageTemplateService.get(company, id4);

        Assertions.assertThat(db1.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db2.get().isDefaultTemplate()).isTrue();
        Assertions.assertThat(db3.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db4.get().isDefaultTemplate()).isTrue();

        messageTemplateService.update(company, db2.get().toBuilder().message("updated").defaultTemplate(false).build());
        messageTemplateService.update(company, db3.get().toBuilder().message("updated").defaultTemplate(true).build());

        db1 = messageTemplateService.get(company, id);
        db2 = messageTemplateService.get(company, id2);
        db3 = messageTemplateService.get(company, id3);
        db4 = messageTemplateService.get(company, id4);

        Assertions.assertThat(db1.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db2.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db3.get().isDefaultTemplate()).isTrue();
        Assertions.assertThat(db4.get().isDefaultTemplate()).isFalse();
    }

    @Test
    public void testListByFilter() throws SQLException {
        var template = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.EMAIL)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_CREATED)
                .message("test")
                .name("t1")
                .system(true)
                .botName("")
                .build();
        var id = messageTemplateService.insert(company, template);
        Assertions.assertThat(id).isNotBlank();
        template = template.toBuilder().id(id).build();

        var template2 = MessageTemplate.builder()
                .defaultTemplate(false)
                .type(TemplateType.EMAIL)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_CREATED)
                .message("test")
                .name("t2")
                .system(true)
                .botName("")
                .build();
        var id2 = messageTemplateService.insert(company, template2);
        Assertions.assertThat(id2).isNotBlank();
        template2 = template2.toBuilder().id(id2).build();

        var template3 = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.SLACK)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_NOTIFIED)
                .message("test")
                .name("t3")
                .system(true)
                .botName("b3")
                .build();
        var id3 = messageTemplateService.insert(company, template3);
        template3 = template3.toBuilder().id(id3).build();

        var db1 = messageTemplateService.get(company, id);
        var db2 = messageTemplateService.get(company, id2);
        var db3 = messageTemplateService.get(company, id3);

        Assertions.assertThat(db1.get().isDefaultTemplate()).isTrue();
        Assertions.assertThat(db2.get().isDefaultTemplate()).isFalse();
        Assertions.assertThat(db3.get().isDefaultTemplate()).isTrue();

        DbListResponse<MessageTemplate> dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                List.of(TemplateType.EMAIL), null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template, template2));

        dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                List.of(TemplateType.SLACK), null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template3));

        dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                null, true, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template, template3));

        dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                null, false, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template2));

        dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                null, null, List.of(EventType.ASSESSMENT_CREATED));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(2, dbListResponse.getCount().intValue());
        Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template, template2));

        dbListResponse = messageTemplateService.listByFilter(company, null, null, 0, 100,
                null, null, List.of(EventType.ASSESSMENT_NOTIFIED));
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getCount().intValue());
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        verifyRecords(dbListResponse.getRecords(), List.of(template3));
    }

    @Test
    public void testBulkDelete() throws SQLException {
        MessageTemplate template1 = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.EMAIL)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_CREATED)
                .message("test")
                .name("t1")
                .system(true)
                .botName("")
                .build();
        String id1 = messageTemplateService.insert(company, template1);
        MessageTemplate template2 = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.EMAIL)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_CREATED)
                .message("test")
                .name("t2")
                .system(true)
                .botName("")
                .build();
        String id2 = messageTemplateService.insert(company, template2);
        MessageTemplate template3 = MessageTemplate.builder()
                .defaultTemplate(true)
                .type(TemplateType.SLACK)
                .emailSubject("me")
                .eventType(EventType.ASSESSMENT_CREATED)
                .message("test")
                .name("t3")
                .system(true)
                .botName("b3")
                .build();
        String id3 = messageTemplateService.insert(company, template3);
        messageTemplateService.bulkDelete(company, List.of(id1, id2, id3));
        Assertions.assertThat(messageTemplateService.get(company, id1)).isEmpty();
        Assertions.assertThat(messageTemplateService.get(company, id2)).isEmpty();
        Assertions.assertThat(messageTemplateService.get(company, id3)).isEmpty();
    }
}