package io.levelops.api.utils;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema.Field;
import io.levelops.commons.databases.services.IntegrationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
@Service
public class OrgUserCsvParser {

    private static final Boolean USE_STRICT_MATCH = true;
    
    private final IntegrationService integrationService;

    @Autowired
    public OrgUserCsvParser(final IntegrationService integrationService){
        this.integrationService = integrationService;
    }
    
    // private Record record;

    public Stream<DBOrgUser> map(final String company, final InputStream source, final OrgUserSchema schema) throws UnsupportedEncodingException {
        var reader = new InputStreamReader(source, "UTF-8");
        var settings = new CsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        
        settings.setHeaderExtractionEnabled(true);
        // settings.process
        var parser = new CsvParser(settings);
        parser.beginParsing(reader);
        for (int i = 0; i < parser.getContext().parsedHeaders().length; i++){
            parser.getContext().parsedHeaders()[i] = parser.getContext().parsedHeaders()[i].trim().toLowerCase();
        }
        var headers = Arrays.asList(parser.getContext().parsedHeaders()).stream()
            .map(item -> item.toLowerCase().trim())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        if (!headers.containsAll(Set.of("email", "full_name"))) {
            return Stream.empty();
        }
        var integrationHeaders = headers.stream()
            .filter(header -> header.startsWith("integration:"))
            .collect(Collectors.toSet());
        var schemaKeys = schema.getFields() == null ? Set.<String>of() : schema.getFields().stream()
            .filter(i -> i != null)
            .map(Field::getKey)
            .filter(StringUtils::isNotBlank)
            .map(String::toLowerCase).collect(Collectors.toSet());
        var customFieldsHeaders = headers.stream()
            .filter(header -> !header.startsWith("integration:"))
            .filter(header -> !header.equalsIgnoreCase("email"))
            .filter(header -> !header.equalsIgnoreCase("full_name"))
            .filter(header -> !header.equalsIgnoreCase("id"))
            .filter(header -> schemaKeys.contains(header))
            .collect(Collectors.toSet());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<DBOrgUser>(){
            Record record = null;

            @Override
            public boolean hasNext() {
                record = parser.parseNextRecord();
                if(record != null) {
                    return true;
                }
                parser.stopParsing();
                return false;
            }

            @Override
            public DBOrgUser next() {
                // record = parser.parseNextRecord();
                if (record == null || StringUtils.isBlank(record.getString("email"))) return null;
                var ids = new HashSet<LoginId>();
                var customFields = new HashMap<String, Object>();
                customFieldsHeaders.stream()
                    .forEach(field -> customFields.put(field, record.getString(field)));
                integrationHeaders.forEach(integration -> {
                    var integrationId = 0;
                    if(record.getString(integration) == null || StringUtils.isBlank(record.getString(integration))){
                        return;
                    }
                    try {
                        var integrationResults = integrationService.listByFilter(company, integration.substring(12).trim(), USE_STRICT_MATCH, null, null, null, null, 0, 1);
                        if (integrationResults != null) {
                            if (integrationResults.getTotalCount() < 1) {
                                log.error("[{}] Unable to find the {}", company, integration);
                                return;
                            }
                            integrationId = Integer.parseInt(integrationResults.getRecords().get(0).getId());
                        }
                    } catch (SQLException e) {
                        log.error("[{}] Unable to find the {}", company, integration, e);
                        return;
                    }
                    ids.add(LoginId.builder()
                    .integrationId(integrationId)
                    .cloudId(record.getString(integration))
                    .username(record.getString(integration))
                    .build());
                });
                return DBOrgUser.builder()
                    .email(record.getString("email"))
                    .fullName(record.getString("full_name"))
                    .ids(ids)
                    .customFields(customFields)
                    .build();
            }
            
        }, Spliterator.ORDERED), false);
    }
}
