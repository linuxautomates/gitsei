package io.levelops.integrations.jira.converters;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraCustomFieldConverterTest {

    @Test
    public void parseValue() {
        assertThat(JiraCustomFieldConverter.parseValue("a")).isEqualTo("a");
        assertThat(JiraCustomFieldConverter.parseValue("012345678901234567890123456789012345678901234567890123456789")).isEqualTo("01234567890123456789012345678901234567890123456789");
        assertThat(JiraCustomFieldConverter.parseValue(123)).isEqualTo("123");
        assertThat(JiraCustomFieldConverter.parseValue(Map.of("value", "a"))).isEqualTo("a");
        assertThat(JiraCustomFieldConverter.parseValue(Map.of("value", 123))).isEqualTo("123");

        assertThat(JiraCustomFieldConverter.parseValue("<p><b>Cross-Domain Dependencies:</b></p>\r\n\r\n<ul>\r\n\t<li>eForm for Credit Card Remittance</li>\r\n\t<li>Common Services Printing from Flex Workflow to Drive Printers</li>\r\n\t<li>QA, Performance, and Production ready environments of Flex-Drive POC</li>\r\n</ul>\r\n")).isEqualTo("<p><b>Cross-Domain Dependencies:</b></p>\r\n\r\n<ul>\r\n");
        assertThat(JiraCustomFieldConverter.parseValueWithoutTruncation("<p><b>Cross-Domain Dependencies:</b></p>\r\n\r\n<ul>\r\n\t<li>eForm for Credit Card Remittance</li>\r\n\t<li>Common Services Printing from Flex Workflow to Drive Printers</li>\r\n\t<li>QA, Performance, and Production ready environments of Flex-Drive POC</li>\r\n</ul>\r\n")).isEqualTo("<p><b>Cross-Domain Dependencies:</b></p>\r\n\r\n<ul>\r\n\t<li>eForm for Credit Card Remittance</li>\r\n\t<li>Common Services Printing from Flex Workflow to Drive Printers</li>\r\n\t<li>QA, Performance, and Production ready environments of Flex-Drive POC</li>\r\n</ul>\r\n");

        HashMap<Object, Object> map = new HashMap<>();
        map.put("value", null);
        assertThat(JiraCustomFieldConverter.parseValue(map)).isEqualTo(null);
    }
}