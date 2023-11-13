import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.integrations.salesforce.models.Solution;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SolutionResponseTest {
    private static final String RESPONSE_FILE_NAME = "solutions.csv";

    @Test
    public void deSerialize() throws IOException {
        List<Solution> solutions = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Solution.class).withHeader();
        MappingIterator<Solution> mappingIterator = mapper
                .reader(schema)
                .forType(Solution.class)
                .readValues(ClassLoader.getSystemResource(RESPONSE_FILE_NAME));

        while (mappingIterator.hasNext()) {
            solutions.add(mappingIterator.next());
        }
        assertThat(solutions.size()).isEqualTo(1);
    }
}
