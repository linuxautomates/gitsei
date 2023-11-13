package io.levelops.aggregations.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.regex.RegexService;
import io.levelops.events.clients.EventsClient;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log4j2
public class TriageLocalServiceTest {

    private final static String company = "test";
    private final static String contents = "MY FILE CONTENTS. hello test";
    private static TriageLocalService triageService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static EventsClient eventsClient;
    private static RegexService regexService;
    private static Storage storage;
    private static CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private static RedisConnectionFactory redisConnectionFactory;
    private static CiCdService ciCdService;

    @BeforeClass
    public static void setup() throws SQLException {
        triageRulesService = Mockito.mock(TriageRulesService.class);
        triageRuleHitsService = Mockito.mock(TriageRuleHitsService.class);
        storage = Mockito.mock(Storage.class);
        eventsClient = Mockito.mock(EventsClient.class);
        regexService = new RegexService();
        jobRunStageDatabaseService = Mockito.mock(CiCdJobRunStageDatabaseService.class);
        ciCdService = Mockito.mock(CiCdService.class);


        var redis = Mockito.mock(RedisConnection.class);
        // local testing
        // var redisHost = "localhost";
        // var redisPort = 6379;
        // RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        // redisConnectionFactory = new JedisConnectionFactory(config);
        redisConnectionFactory = Mockito.mock(JedisConnectionFactory.class);
        triageService = new TriageLocalService("jenkinsLogsBucket", triageRulesService, triageRuleHitsService, regexService, storage, eventsClient, jobRunStageDatabaseService, redisConnectionFactory, ciCdService);

        when(storage.readAllBytes(any(BlobId.class))).thenReturn(contents.getBytes());

        when(triageRulesService.list(eq(company), any(), any(), any(), any(), eq(0), anyInt()))
                .thenReturn(DbListResponse.of(List.of(TriageRule.builder()
                        .name("name")
                        .regexes(List.of("hello"))
                        .build()), 1));
        when(triageRulesService.list(eq(company), any(), any(), any(), any(), eq(1), anyInt()))
                .thenReturn(DbListResponse.of(List.of(), 0));
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.incrBy(any(), anyLong())).thenReturn(0L);
    }


    @Test
    public void testAnalyzeJenkinsGCSLogs() throws IOException, SQLException {
        var instanceId = "";
        var instanceName = "";
        var jobName = "";
        var jobStatus = "";
        var jobId = "";
        var jobRunId = "";
        var stageId = "";
        var gcsLogsLocation = "";
        var logBucket = "";
        var url = "";
        var stepId = "";
        triageService.analyzeJenkinsGCSLogs(company, instanceId, instanceName, jobId, jobName, jobStatus, jobRunId, stageId, stepId, gcsLogsLocation, logBucket, url);

        verify(triageRuleHitsService, atLeastOnce()).insert(any(), any());
        verify(triageRuleHitsService, atMost(2)).insert(any(), any());
    }

    @Test
    public void testAnalyzeJenkinsLogsContents() throws IOException, SQLException {
        var instanceId = "";
        var instanceName = "";
        var jobName = "";
        var jobStatus = "";
        var jobId = "";
        var jobRunId = "";
        var stageId = "";
        var url = "";
        var stepId = "";
        triageService.analyzeJenkinsLogsContents(company, instanceId, instanceName, jobId, jobName, jobStatus, jobRunId, stageId, stepId, contents, url);
        verify(triageRuleHitsService, atLeastOnce()).insert(any(), any());
        verify(triageRuleHitsService, atMost(2)).insert(any(), any());
    }
}