import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.integrations.salesforce.models.Case;
import io.levelops.integrations.salesforce.models.CaseComment;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CaseCommentResponseTest {
    private static final String RESPONSE_FILE_NAME = "case_comments.csv";

    @Test
    public void deSerialize() throws IOException {
        List<CaseComment> caseComments = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(CaseComment.class).withHeader();
        MappingIterator<CaseComment> mappingIterator = mapper
                .reader(schema)
                .forType(CaseComment.class)
                .readValues(ClassLoader.getSystemResource(RESPONSE_FILE_NAME));

        while (mappingIterator.hasNext()) {
            caseComments.add(mappingIterator.next());
        }
        assertThat(caseComments.size()).isEqualTo(3);
    }
}
