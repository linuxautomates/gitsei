package io.levelops.internal_api.services.plugins.preprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.ConfigTableUtils;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.jackson.CsvParsing;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.internal_api.services.plugins.PluginResultsService;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Log4j2
@Service
public class CsvPluginResultPreProcessService {

    private final ObjectMapper objectMapper;
    private final PluginResultsService pluginResultsService;

    @Autowired
    public CsvPluginResultPreProcessService(ObjectMapper objectMapper, PluginResultsService pluginResultsService) {
        this.objectMapper = objectMapper;
        this.pluginResultsService = pluginResultsService;
    }

    public void preprocess(String company, final UUID resultId, MultipartFile jsonFile, MultipartFile resultFile, PluginResultDTO pluginResultDTO) throws Exception {
        try (InputStream inputStream = resultFile.getInputStream()) {
            Stream<Map<String, String>> csvStream = CsvParsing.parseToStream(inputStream);
            ConfigTable configTable = ConfigTableUtils.fromCsv(csvStream, null, true);

            PluginResultDTO dtoWithResults = pluginResultDTO.toBuilder()
                    .results(ParsingUtils.toJsonObject(objectMapper, configTable))
                    .build();

            pluginResultsService.createPluginResult(company, dtoWithResults,  null, resultId, null);
        }
    }

}
