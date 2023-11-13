package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.BestPracticesItem.BestPracticeType;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BestPracticesServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static BestPracticesService bPracticeService;
    private static TagItemDBService tagItemDBService;
    private static TagsService tagsService;

    private static DataSource dataSource;
    private static String company = "test";

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        bPracticeService = new BestPracticesService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        tagsService = new TagsService(dataSource);
        bPracticeService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
    }

    @Test
    public void testUpdate() throws SQLException {
        var id = bPracticeService.insert(company, BestPracticesItem.builder().name("test").type(BestPracticeType.LINK).value("https://test.io").build());
        Assert.assertEquals("test", bPracticeService.get(company, id).get().getName());
        
        bPracticeService.update(company, BestPracticesItem.builder().id(UUID.fromString(id)).name("test1").type(BestPracticeType.LINK).value("https://test1.io").build());
        Assert.assertEquals("test1", bPracticeService.get(company, id).get().getName());
        
        bPracticeService.update(company, BestPracticesItem.builder().id(UUID.fromString(id)).name("test2").value("https://test2.io").build());
        Assert.assertEquals("test2", bPracticeService.get(company, id).get().getName());
        
        bPracticeService.update(company, BestPracticesItem.builder().id(UUID.fromString(id)).name("test3").build());
        Assert.assertEquals("test3", bPracticeService.get(company, id).get().getName());
    }

    private static BestPracticesItem createBestPracticesItem(int i){
        BestPracticesItem item = BestPracticesItem.builder()
                .name("test-" + i)
                .type(BestPracticeType.FILE)
                .value("file-id-" +i)
                .metadata("file-name-" + i)
                .build();
        return item;
    }

    private void verifyRecord(BestPracticesItem expected, BestPracticesItem actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getType(), actual.getType());
        Assert.assertEquals(expected.getValue(), actual.getValue());
        Assert.assertEquals(expected.getMetadata(), actual.getMetadata());
        Assert.assertEquals(CollectionUtils.isEmpty(expected.getTags()), CollectionUtils.isEmpty(actual.getTags()));
        if (CollectionUtils.isNotEmpty(expected.getTags())){
            Assert.assertEquals(expected.getTags(), actual.getTags());
        }
        Assert.assertNotNull(actual.getCreatedAt());
        Assert.assertNotNull(actual.getUpdatedAt());
    }

    @Test
    public void testUpdateWithMetadata() throws SQLException {
        BestPracticesItem item = createBestPracticesItem(0);
        var id = UUID.fromString(bPracticeService.insert(company, item));
        item = item.toBuilder().id(id).build();
        verifyRecord(item, bPracticeService.get(company, id.toString()).get());

        item = createBestPracticesItem(1).toBuilder().id(id).build();
        bPracticeService.update(company, item);
        verifyRecord(item, bPracticeService.get(company, id.toString()).get());

        item = createBestPracticesItem(2).toBuilder().id(id).build();
        bPracticeService.update(company, item);
        verifyRecord(item, bPracticeService.get(company, id.toString()).get());

        item = createBestPracticesItem(3).toBuilder().id(id).value(null).build();
        bPracticeService.update(company, item);
        item = item.toBuilder().value("file-id-2").build();
        verifyRecord(item, bPracticeService.get(company, id.toString()).get());
    }

    @Test
    public void testBulkDelete() throws SQLException {
        String id1 = bPracticeService.insert(company, createBestPracticesItem(1));
        String id2 = bPracticeService.insert(company, createBestPracticesItem(2));
        String id3 = bPracticeService.insert(company, createBestPracticesItem(3));
        List<String> tags = List.of("tag1");
        List<String> tagIds = tagsService.insert(company, tags);
        String id = tagItemDBService.insert(company, TagItemMapping.builder().tagId(tagIds.get(0)).itemId(id1).tagItemType(TagItemMapping.TagItemType.BEST_PRACTICE).build());
        bPracticeService.bulkDelete(company, List.of(id1, id2, id3));
        Assertions.assertThat(bPracticeService.get(company, id1)).isEmpty();
        Assertions.assertThat(tagItemDBService.get(company, id)).isEmpty();
        Assertions.assertThat(bPracticeService.get(company, id2)).isEmpty();
        Assertions.assertThat(bPracticeService.get(company, id3)).isEmpty();
    }
}