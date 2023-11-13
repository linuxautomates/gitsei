package io.levelops.commons.jackson;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CsvParsing {

    private static volatile CsvMapper csvMapper = null;

    public static CsvMapper getDefaultCsvMapper() {
        if (csvMapper != null) {
            return csvMapper;
        }
        synchronized (CsvParsing.class) {
            if (csvMapper != null) {
                return csvMapper;
            }
            csvMapper = new CsvMapper();
            return csvMapper;
        }
    }

    public static Stream<Map<String, String>> parseToStream(InputStream inputStream) throws IOException {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(parse(inputStream), Spliterator.ORDERED), false);
    }

    public static MappingIterator<Map<String, String>> parse(InputStream inputStream) throws IOException {
        return parse(inputStream, CsvSchema.emptySchema().withHeader());
    }

    public static MappingIterator<Map<String, String>> parse(InputStream inputStream, CsvSchema schema) throws IOException {
        return getDefaultCsvMapper()
                .readerFor(Map.class)
                .with(schema)
                .readValues(inputStream);
    }

    public static List<String> extractColumnsFromHeader(MappingIterator<?> mappingIterator) {
        if (mappingIterator == null || !(mappingIterator.getParserSchema() instanceof CsvSchema)) {
            return Collections.emptyList();
        }
        CsvSchema schema = (CsvSchema) mappingIterator.getParserSchema();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(schema.iterator(), Spliterator.ORDERED), false)
                .sorted(Comparator.comparingInt(CsvSchema.Column::getIndex))
                .map(CsvSchema.Column::getName)
                .collect(Collectors.toList());
    }

}
