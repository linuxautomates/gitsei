import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.integrations.salesforce.models.Contract;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ContractResponseTest {
    private static final String RESPONSE_FILE_NAME = "contracts.csv";

    @Test
    public void deSerialize() throws IOException {
        List<Contract> contracts = new ArrayList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Contract.class).withHeader();
        MappingIterator<Contract> mappingIterator = mapper
                .reader(schema)
                .forType(Contract.class)
                .readValues(ClassLoader.getSystemResource(RESPONSE_FILE_NAME));

        while (mappingIterator.hasNext()) {
            contracts.add(mappingIterator.next());
        }
        assertThat(contracts.size()).isEqualTo(1);
    }
}
