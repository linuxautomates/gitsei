package io.levelops.commons.databases.converters;

import io.levelops.integrations.azureDevops.models.Fields;
import org.junit.Test;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureDevopsCustomFieldConvertersTest {

    @Test
    public void parseDate() throws ParseException {
        assertThat(AzureDevopsCustomFieldConverters.parseDate("2021-08-16")).isEqualTo("2021-08-16");
    }

    @Test
    public void parseDateTime() {
        assertThat(AzureDevopsCustomFieldConverters.parseDateTime("2007-12-03T10:15:30.01Z"))
                .isEqualTo(Long.valueOf("1196676930"));
        assertThat(AzureDevopsCustomFieldConverters.parseDateTime("2007-12-03T10:15:30.012Z"))
                .isEqualTo(Long.valueOf("1196676930"));
        assertThat(AzureDevopsCustomFieldConverters.parseDateTime("2007-12-03T10:15:30.0123Z"))
                .isEqualTo(Long.valueOf("1196676930"));
        assertThat(AzureDevopsCustomFieldConverters.parseDateTime("2007-12-03T10:15:30.012+0000"))
                .isEqualTo(Long.valueOf("1196676930"));
    }

    @Test
    public void parseValue() {
        assertThat(AzureDevopsCustomFieldConverters.parseValue("a", null)).isEqualTo("a");
        assertThat(AzureDevopsCustomFieldConverters.parseValue("012345678901234567890123456789012345678901234567890123456789", null))
                .isEqualTo("01234567890123456789012345678901234567890123456789");
        assertThat(AzureDevopsCustomFieldConverters.parseValue(123, null)).isEqualTo("123");
        assertThat(AzureDevopsCustomFieldConverters.parseValue(true, null)).isEqualTo("true");
        assertThat(AzureDevopsCustomFieldConverters.parseValue(Boolean.TRUE, null)).isEqualTo("true");
        assertThat(AzureDevopsCustomFieldConverters.parseValue(Fields.AuthorizationDetail.builder().uniqueName("uniq_name").build(), null))
                .isEqualTo("uniq_name");
        assertThat(AzureDevopsCustomFieldConverters.parseValue("2007-12-03T10:15:30.012+0000","dateTime"))
                .isEqualTo(Long.valueOf("1196676930"));
    }

}
