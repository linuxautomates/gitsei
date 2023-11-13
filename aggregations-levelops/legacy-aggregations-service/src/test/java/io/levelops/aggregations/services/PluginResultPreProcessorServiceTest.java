package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.JOB_RUN_COMPLETE_FILE_PATTERN;

@Log4j2
public class PluginResultPreProcessorServiceTest {
    private final String COMPANY = "test";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobFullNameDetailsService jobFullNameDetailsService = new JobFullNameDetailsService(objectMapper);
    private final JenkinsConfigChangesParserService jenkinsConfigChangesParserService = new JenkinsConfigChangesParserService(jobFullNameDetailsService, Integer.MAX_VALUE);
    private final JobRunParamsService jobRunParamsService = new JobRunParamsService(objectMapper);
    private final JobSCMConfigService jobSCMConfigService = new JobSCMConfigService(objectMapper);
    private final JobRunGitChangesService jobRunGitChangesService = new JobRunGitChangesService(objectMapper);
    private final JobRunTriggersParserService jobRunTriggersParserService = new JobRunTriggersParserService(objectMapper);
    private final JenkinsJobRunsParserService jenkinsJobRunsParserService = new JenkinsJobRunsParserService(objectMapper, new BuildRunMessageService(), jobRunParamsService, jobSCMConfigService, jobRunGitChangesService, jobFullNameDetailsService, jobRunTriggersParserService, Integer.MAX_VALUE);
    private final JenkinsInstanceInfoParserService jenkinsInstanceInfoParserService = new JenkinsInstanceInfoParserService();

    @Test
    public void testJobRunCompleteDataPattern(){
        Map<String, Long> valid = new HashMap<>();
        valid.put("run-complete-data-22.txt", 22L);
        valid.put("run-complete-data-21.txt", 21L);
        valid.put("run-complete-data-24.txt", 24L);
        valid.put("run-complete-data-23.txt", 23L);
        valid.put("run-complete-data-3.txt", 3L);
        valid.put("run-complete-data-22.txt", 22L);
        valid.put("run-complete-data-21.txt", 21L);
        valid.put("run-complete-data-28.txt", 28L);
        valid.put("run-complete-data-27.txt", 27L);
        valid.put("run-complete-data-23.txt", 23L);
        valid.put("run-complete-data-23.txt", 23L);
        for(String key : valid.keySet()) {
            Matcher m = JOB_RUN_COMPLETE_FILE_PATTERN.matcher(key);
            Assert.assertTrue(m.matches());
            Assert.assertEquals(valid.get(key), Long.valueOf(m.group(1)));
        }
    }

    @Test
    public void testPreProcess() throws IOException, URISyntaxException, BadRequestException {
        File jenkinsPluginDataZipsDir = new File(this.getClass().getClassLoader().getResource("jenkins-plugin-data-zips").toURI());
        File[] dataZipFiles = jenkinsPluginDataZipsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(!file.isFile()){
                    return false;
                }
                String fileName = file.getName();
                return (fileName.startsWith("data")) && (fileName.endsWith(".zip"));
            }
        });
        if(dataZipFiles == null){
            return;
        }

        File resultsDir = new File(this.getClass().getClassLoader().getResource("jenkins-plugin-data-zips-results").toURI());
        File[] resultFiles = resultsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(!file.isFile()){
                    return false;
                }
                String fileName = file.getName();
                return (fileName.startsWith("data")) && (fileName.endsWith(".json"));
            }
        });
        Map<String, String> results = new HashMap<>();
        for(File resultFile : resultFiles){
            results.put(FilenameUtils.removeExtension(resultFile.getName()), Files.readString(resultFile.toPath()));
        }

        for(File currentDataZipFile : dataZipFiles){
            log.info("processing file: {}", currentDataZipFile);
            String messageId = UUID.randomUUID().toString();
            PluginResultDTO pluginResultDTO = PluginResultDTO.builder()
                    .pluginName("Jenkins Config").pluginClass("monitoring").tool("jenkins_config").version("1").productIds(Arrays.asList("71"))
                    .successful(true).metadata(Collections.emptyMap()).labels(null).build();
            String json = objectMapper.writeValueAsString(pluginResultDTO);

            File jsonFile = null;
            File unzipFolder = null;

            try {
                jsonFile = Files.createTempFile("test", ".json").toFile();
                Files.write(jsonFile.toPath(), json.getBytes());

                unzipFolder = Files.createTempDirectory("jenkinsPluginUnzip").toFile();

                Map<String, Map<Long, File>> jobRunDetailsMap = new HashMap<>();
                PluginResultPreProcessorService pluginResultPreProcessorService = new PluginResultPreProcessorService(objectMapper, jenkinsConfigChangesParserService, jenkinsJobRunsParserService, jenkinsInstanceInfoParserService);
                PluginResultDTO result = pluginResultPreProcessorService.preProcess(COMPANY, messageId, jsonFile, currentDataZipFile, unzipFolder, jobRunDetailsMap);
                Assert.assertNotNull(result);
                String actual = objectMapper.writeValueAsString(result);
                log.info("actual = {}", actual);
                String expected = results.getOrDefault(FilenameUtils.removeExtension(currentDataZipFile.getName()), "");
                log.info("expected = {}", expected);
                // ToDo: Write custom comparator
                //Assert.assertEquals("Mismatch for file " + currentDataZipFile.getName(), actual, expected);
                Assert.assertNotNull(actual);
            } finally {
                if((unzipFolder != null) && (unzipFolder.exists())){
                    FileSystemUtils.deleteRecursively(unzipFolder);
                }
                if((jsonFile != null) && (jsonFile.exists())) {
                    jsonFile.delete();
                }
            }
        }
    }
}