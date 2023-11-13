import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.integrations.salesforce.models.CaseHistory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseHistoryResponseTest {
    private static final String RESPONSE_FILE_NAME = "case_histories.csv";

    @Test
    public void deSerialize() throws IOException {
        List<CaseHistory> caseHistories = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(CaseHistory.class).withHeader();
        MappingIterator<CaseHistory> mappingIterator = mapper
                .reader(schema)
                .forType(CaseHistory.class)
                .readValues(ClassLoader.getSystemResource(RESPONSE_FILE_NAME));

        while (mappingIterator.hasNext()) {
            caseHistories.add(mappingIterator.next());
        }
        assertThat(caseHistories.size()).isEqualTo(5);
    }
}
