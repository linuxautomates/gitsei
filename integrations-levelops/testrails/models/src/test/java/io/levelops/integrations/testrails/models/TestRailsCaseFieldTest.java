package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsCaseFieldTest {
    private static final String RESPONSE_FILE_NAME = "case-fields.json";

    private static final Map<Integer, CaseField.FieldType> FIELD_TYPES = new HashMap<>(){{
        put(1, CaseField.FieldType.STRING);
        put(2, CaseField.FieldType.INTEGER);
        put(3, CaseField.FieldType.TEXT);
        put(4, CaseField.FieldType.URL);
        put(5, CaseField.FieldType.CHECKBOX);
        put(6, CaseField.FieldType.DROPDOWN);
        put(7, CaseField.FieldType.USER);
        put(8, CaseField.FieldType.DATE);
        put(9, CaseField.FieldType.MILESTONE);
        put(10, CaseField.FieldType.STEPS);
        put(12, CaseField.FieldType.MULTI_SELECT);
        put(13, CaseField.FieldType.SCENARIO);
    }};

    @Test
    public void deSerialize() throws IOException {
        List<CaseField> caseFields = DefaultObjectMapper.get()
                .readValue(ResourceUtils.getResourceAsStream(RESPONSE_FILE_NAME), DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, CaseField.class));
        DefaultObjectMapper.prettyPrint(caseFields);
        assertThat(caseFields).isNotNull();
        assertThat(caseFields).isNotEmpty();
        caseFields.forEach(caseField -> assertThat(caseField.getType()).isEqualTo(FIELD_TYPES.get(caseField.getTypeId())));
    }
}
