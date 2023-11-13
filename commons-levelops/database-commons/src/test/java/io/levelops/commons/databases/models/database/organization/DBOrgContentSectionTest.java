package io.levelops.commons.databases.models.database.organization;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ResourceUtils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class DBOrgContentSectionTest {
    
    @Test
    public void testParsing() throws IOException {
        var json = ResourceUtils.getResourceAsString("organization/sections.json");
        // var results = ParsingUtils.parseList(DefaultObjectMapper.get(), "sections", DBOrgContentSection.class, json);
        var results = ParsingUtils.parseSet(DefaultObjectMapper.get(), "sections", DBOrgContentSection.class, json);
        Assertions.assertThat(results).hasSize(1);
    }
}
