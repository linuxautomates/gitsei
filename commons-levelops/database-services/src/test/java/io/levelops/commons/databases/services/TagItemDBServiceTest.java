package io.levelops.commons.databases.services;


import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TagItemDBServiceTest {
    private static final String company = "test";
    private static final TagItemMapping.TagItemType ITEM_TYPE = TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE;

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private TagsService tagsService;
    private TagItemDBService dbService;



    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        var template = new NamedParameterJdbcTemplate(dataSource);
        List.of(
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        ).forEach(template.getJdbcTemplate()::execute);

        tagsService = new TagsService(dataSource);
        dbService = new TagItemDBService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        tagsService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        int questionnaireTemplatesSize = 3;
        int multipicationFactor = 2;
        String tagValuePrefix = "tag-value";
        List<UUID> questionnaireTemplateIds = new ArrayList<>();
        for(int i=0; i <questionnaireTemplatesSize; i++){
            questionnaireTemplateIds.add(UUID.randomUUID());
        }
        Map<String, List<String>> expected = new HashMap<>();
        List<TagItemMapping> mappings = new ArrayList<>();
        for (int i = 0; i < (questionnaireTemplateIds.size()*multipicationFactor); i++) {
            String itemId = questionnaireTemplateIds.get(i % questionnaireTemplateIds.size()).toString();
            String tagValue = tagValuePrefix +i;
            expected.computeIfAbsent(itemId, k -> new ArrayList<>()).add(tagValue);
            Tag tag = Tag.builder().name(tagValue).build();
            String tagId = tagsService.insert(company, tag);
            Assert.assertNotNull(tagId);
            TagItemMapping mapping = TagItemMapping.builder()
                    .tagId(tagId)
                    .itemId(itemId)
                    .tagItemType(ITEM_TYPE)
                    .build();
            mappings.add(mapping);
        }
        List<String> tagItemIds = dbService.batchInsert(company, mappings);
        Assert.assertTrue(CollectionUtils.isNotEmpty(tagItemIds));

        for(int i=0; i <questionnaireTemplatesSize; i++){
            DbListResponse<Pair<String,String>> dbListResponse = dbService.listTagIdsForItem(company, ITEM_TYPE, List.of(questionnaireTemplateIds.get(i).toString()),0, 500);
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(2, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(2, dbListResponse.getCount().intValue());
            Assert.assertEquals(2, dbListResponse.getRecords().size());
            for(int j=0; j<multipicationFactor; j++){
                int n = i + (j*questionnaireTemplatesSize);
                Assert.assertEquals(questionnaireTemplateIds.get(i).toString(),dbListResponse.getRecords().get(j).getLeft());
                Assert.assertEquals(tagValuePrefix + n, dbListResponse.getRecords().get(j).getRight());
            }
        }

        DbListResponse<Pair<String,String>> dbListResponse = dbService.listTagIdsForItem(company, ITEM_TYPE, questionnaireTemplateIds.stream().map(UUID::toString).collect(Collectors.toList()), 0, 500);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(6, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(6, dbListResponse.getCount().intValue());
        Assert.assertEquals(6, dbListResponse.getRecords().size());
        Map<String, List<String>> actual = dbListResponse.getRecords().stream().collect(
                Collectors.groupingBy(Pair::getLeft, HashMap::new,
                        Collectors.mapping(Pair::getRight, Collectors.toList()))
        );
        for(String k : expected.keySet()){
            Assert.assertEquals(expected.get(k), actual.get(k));
        }
    }

    @Test
    public void listTagsForItemTest() throws SQLException {
        String id = "1";
        Tag tag = Tag.builder().id("1").name("tag1").build();
        String tagId = tagsService.insert(company, tag);
        
        dbService.insert(company, TagItemMapping.builder().tagId(tagId).itemId(id).tagItemType(TagItemType.PLUGIN_RESULT).build());
        var results = dbService.listTagsForItem(company, TagItemType.PLUGIN_RESULT, id, 0, 20);
        Assert.assertNotNull(results);
        assertThat(results.getRecords()).containsExactlyInAnyOrder(tag);
    }

    @Test
    public void listItemIdsForTagIdsTest() throws SQLException {
        List<String> tags = List.of("tag1", "tag2");
        List<String> tagIds = tagsService.insert(company, tags);

        List<String> itemIds = List.of("itemId1", "itemId2");
        dbService.batchInsert(company, tagIds.stream().map( tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(0)).tagItemType(TagItemType.PLUGIN_RESULT).build()).collect(Collectors.toList()));
        dbService.batchInsert(company, tagIds.stream().map( tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(1)).tagItemType(TagItemType.PLUGIN_RESULT).build()).collect(Collectors.toList()));
        var results = dbService.listItemIdsForTagIds(company, TagItemType.PLUGIN_RESULT, tagIds, 0, 20);
        Assert.assertNotNull(results);
        assertThat(results.getRecords()).containsExactlyInAnyOrderElementsOf(itemIds);
    }

    @Test
    public void testBulkDelete() throws SQLException {
        List<String> tags = List.of("tag1", "tag2");
        List<String> tagIds = tagsService.insert(company, tags);
        List<String> itemIds = List.of("1", "2");
        dbService.batchInsert(company, tagIds.stream().map( tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(0)).tagItemType(TagItemType.INTEGRATION).build()).collect(Collectors.toList()));
        dbService.batchInsert(company, tagIds.stream().map( tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(1)).tagItemType(TagItemType.INTEGRATION).build()).collect(Collectors.toList()));
        dbService.bulkDeleteTagsForItems(company, String.valueOf(TagItemType.INTEGRATION), itemIds);
        assertThat(dbService.get(company, itemIds.get(0))).isEmpty();
        assertThat(dbService.get(company, itemIds.get(1))).isEmpty();
    }

    @Test
    public void bulkDelete() throws SQLException {
        List<String> tags = List.of("tag1", "tag2");
        List<String> tagIds = tagsService.insert(company, tags);

        List<String> itemIds = List.of("itemId1", "itemId2");
        List<String> itemIds1 = dbService.batchInsert(company, tagIds.stream().map(tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(0)).tagItemType(TagItemType.PLUGIN_RESULT).build()).collect(Collectors.toList()));
        List<String> itemIds2 = dbService.batchInsert(company, tagIds.stream().map(tagId -> TagItemMapping.builder().tagId(tagId).itemId(itemIds.get(1)).tagItemType(TagItemType.PLUGIN_RESULT).build()).collect(Collectors.toList()));
        var results1 = dbService.listItemIdsForTagIds(company, TagItemType.PLUGIN_RESULT,
                tagIds, 0, 10);
        assertThat(results1.getRecords().size()).isEqualTo(2);
        itemIds1.addAll(itemIds2);
        dbService.bulkDeleteTagsForItems(company, TagItemType.PLUGIN_RESULT.toString(), itemIds1);
        var results = dbService.listItemIdsForTagIds(company, TagItemType.PLUGIN_RESULT,
                tagIds, 1, 10);
        assertThat(results.getRecords()).isEmpty();
    }
}