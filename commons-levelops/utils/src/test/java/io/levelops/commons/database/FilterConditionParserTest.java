package io.levelops.commons.database;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterConditionParserTest {

    @Test
    public void sanitizeParamName() {
        assertThat(FilterConditionParser.sanitizeParamName("a-b>c@d.e:f")).isEqualTo("a_bcd_ef");
    }
}