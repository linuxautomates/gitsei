package io.levelops.commons.databases.services;


import io.levelops.commons.databases.models.database.Tag;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TagDBServiceTest {
    private static final String company = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private TagsService tagsService;



    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        tagsService = new TagsService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        tagsService.ensureTableExistence(company);
    }

    @Test
    public void inserTest() throws SQLException {
        Tag tag = Tag.builder().name("tagValue").build();
        String tagId = tagsService.insert(company, tag);
        assertThat(tagId).isNotBlank();

        Tag dbTag = tagsService.get(company, tagId).orElseThrow();
        assertThat(dbTag.getName()).isEqualTo("tagvalue");
        assertThat(dbTag.getId()).isEqualTo(tagId);
    }

    @Test
    public void batchInsertTest() throws SQLException {
        List<String> tagValues = List.of("tag1", "tag2", "tag3");
        List<String> tagIds = tagsService.insert(company, tagValues);
        assertThat(tagIds).isNotEmpty();

        List<Tag> tags = tagsService.findTagsByValues(company, tagValues);
        assertThat(tags.size()).isEqualTo(tagValues.size());
        assertThat(tags.stream().map(Tag::getName).collect(Collectors.toSet())).containsExactlyInAnyOrderElementsOf(tagValues);
    }

    @Test
    public void forceGetTagIdsTest() throws SQLException {
        List<String> existingTagValues = List.of("tag4", "Tag5", "tag6");
        List<String> existingTagIds = tagsService.insert(company, existingTagValues);
        assertThat(existingTagIds).isNotEmpty();

        List<String> fullTagValues = List.of("tag4", "tag5", "Tag6", "tag7", "tag8", "tag9", "tag10");
        List<String> fullTagIds = tagsService.forceGetTagIds(company, fullTagValues);
        assertThat(fullTagIds.size()).isEqualTo(fullTagValues.size());

        List<Tag> tags = tagsService.findTagsByValues(company, fullTagValues);
        assertThat(tags.size()).isEqualTo(fullTagValues.size());
        assertThat(tags.stream()
                    .map(Tag::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet())
                )
                .containsExactlyInAnyOrderElementsOf(fullTagValues.stream().map(String::toLowerCase).collect(Collectors.toList()));

        List<String> existingTagValues2 = List.of("tag2", "tag4", "tag3");
        List<String> tagIds2 = tagsService.forceGetTagIds(company, existingTagValues2);
        assertThat(tagIds2.size()).isEqualTo(existingTagValues2.size());
    }

    @Test
    public void findTags() throws SQLException {
        List<String> tagValues = List.of("tag4", "Tag5", "taG6");
        List<String> tagIds = tagsService.insert(company, tagValues);
        assertThat(tagIds).isNotEmpty();

        List<String> tagValuesInLowerCases = tagValues.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<Tag> tags = tagsService.findTagsByValues(company, tagValues);
        List<String> dbTagValues = tags.stream().map(Tag::getName).collect(Collectors.toList());
        assertThat(tags.size()).isEqualTo(tagValues.size());
        assertThat(dbTagValues).containsExactlyInAnyOrderElementsOf(tagValuesInLowerCases);
        
        tags = tagsService.findTagsByValues(company, tagValues.stream().map(String::toUpperCase).collect(Collectors.toList()));
        dbTagValues = tags.stream().map(Tag::getName).collect(Collectors.toList());
        assertThat(tags.size()).isEqualTo(tagValues.size());
        assertThat(dbTagValues).containsExactlyInAnyOrderElementsOf(tagValuesInLowerCases);
        
        tags = tagsService.findTagsByValues(company, tagValues.stream().map(String::toLowerCase).collect(Collectors.toList()));
        dbTagValues = tags.stream().map(Tag::getName).collect(Collectors.toList());
        assertThat(tags.size()).isEqualTo(tagValues.size());
        assertThat(dbTagValues).containsExactlyInAnyOrderElementsOf(tagValuesInLowerCases);
    }
}