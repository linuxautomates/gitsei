import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.integrations.salesforce.models.Case;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseResponseTest {
    private static final String RESPONSE_FILE_NAME = "cases.csv";

    @Test
    public void deSerialize() throws IOException {
        List<Case> cases = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Case.class).withHeader();
        MappingIterator<Case> mappingIterator = mapper
                .reader(schema)
                .forType(Case.class)
                .readValues(ClassLoader.getSystemResource(RESPONSE_FILE_NAME));

        while (mappingIterator.hasNext()) {
            cases.add(mappingIterator.next());
        }
        assertThat(cases.size()).isEqualTo(3);
    }
}
