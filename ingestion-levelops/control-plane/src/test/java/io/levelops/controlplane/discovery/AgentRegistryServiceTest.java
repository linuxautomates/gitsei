package io.levelops.controlplane.discovery;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.AgentHandle;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Before;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import org.assertj.core.api.Assertions;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class AgentRegistryServiceTest {
  
  private static AgentRegistryService agentRegistryService;
  private static JedisConnectionFactory redisFactory;
  private static Integer timeoutSecs = 2;
  private static RedisServer redisServer;
  private static Integer redisPort = 6370;
  private AgentHandle agent1;
  private AgentHandle agent2;
  private AgentHandle agent3;
  private AgentHandle agent4;

  private AgentHandle agent5;

  @BeforeClass
  public static void connection(){
    AgentRegistryServiceTest.redisServer = new RedisServer(redisPort);
    AgentRegistryServiceTest.redisServer.start();
    AgentRegistryServiceTest.redisFactory = new JedisConnectionFactory(new RedisStandaloneConfiguration("localhost", redisPort));
    AgentRegistryServiceTest.redisFactory.afterPropertiesSet();
    ExpirableLockRegistry lockRegistry = new LocalLockRegistry();
    AgentRegistryServiceTest.agentRegistryService = new RedisAgentRegistryService(
      DefaultObjectMapper.get(),
      timeoutSecs,
      AgentRegistryServiceTest.redisFactory,
      lockRegistry,
      1, 1, 500, TimeUnit.MILLISECONDS);
  }

  @AfterClass
  public static void stopTest(){
    AgentRegistryServiceTest.redisServer.stop();
  }

  @Before
  public void setup(){
    this.agentRegistryService.clearAll();
    agent1 = AgentHandle.builder()
      .agentId("agent1")
      .agentType("local")
      .controllerNames(Set.of("myController"))
      .integrationIds(List.of("1"))
      .telemetry(Map.of("jdk", "19"))
      .tenantId("ivan")
      .build();
    agent2 = AgentHandle.builder()
      .agentId("agent2")
      .agentType("local")
      .controllerNames(Set.of("myController2"))
      .integrationIds(List.of("1", "2"))
      .telemetry(Map.of("jdk", "19"))
      .tenantId("ivan")
      .build();
    
    agent3 = AgentHandle.builder()
      .agentId("agent3")
      .agentType("local")
      .controllerNames(Set.of("myController"))
      .integrationIds(List.of("1", "2"))
      .telemetry(Map.of("jdk", "19"))
      .tenantId("ivan")
      .build();
    
    agent4 = AgentHandle.builder()
      .agentId("agent4")
      .agentType("local")
      .controllerNames(Set.of("myController"))
      .integrationIds(List.of("1", "2"))
      .telemetry(Map.of("jdk", "19"))
      .tenantId("ivan")
      .build();

    agentRegistryService.registerAgent(agent1);
    agentRegistryService.registerAgent(agent2);
    agentRegistryService.registerAgent(agent3);
    agentRegistryService.registerAgent(agent4);
  }

  @Test
  public void test(){
    log.info("Testing inserts and retrievals");
    var results1 = agentRegistryService.getAgentById("agent1");
    Assertions.assertThat(results1).isPresent();
    Assertions.assertThat(results1.get().getAgentHandle()).isEqualTo(agent1);

    var results2 = agentRegistryService.getAgentsByControllerName("myController");
    Assertions.assertThat(results2).hasSize(3);
    Assertions.assertThat(results2.stream().map(RegisteredAgent::getAgentHandle).toList()).containsExactlyInAnyOrder(agent1, agent3, agent4);

    var results3 = agentRegistryService.getAgentsByControllerName("myController2");
    Assertions.assertThat(results3).hasSize(1);
    Assertions.assertThat(results3.stream().map(RegisteredAgent::getAgentHandle).toList()).containsExactly(agent2);
  }

  @Test
  public void testExpiration() throws InterruptedException{
    var agents = agentRegistryService.getAllAgents();
    Assertions.assertThat(agents).hasSize(4);
    Thread.sleep(TimeUnit.SECONDS.toMillis(timeoutSecs - 1));
    agentRegistryService.refreshHeartbeatAndTelemetry(agent1);
    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    agents = agentRegistryService.getAllAgents();
    Assertions.assertThat(agents).hasSize(1);
    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
    agents = ((RedisAgentRegistryService) agentRegistryService).getAllAgents(true);
    Assertions.assertThat(agents).hasSize(4);
  }

    @Test
    public void testSatelliteAgent() {
        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("log", "TEST_LOGS");
        agent5 = AgentHandle.builder()
                .agentId("agent5")
                .agentType("satellite")
                .controllerNames(Set.of("myControllerTest"))
                .integrationIds(List.of("1", "2"))
                .telemetry(telemetry)
                .tenantId("ivan")
                .build();
        agentRegistryService.registerAgent(agent5);
        Assert.assertNull(agent5.getTelemetry().get("log"));
        Jedis redis = (Jedis) redisFactory.getConnection().getNativeConnection();
        String actualLogs = redis.hget("ingestion_logs_agent", "agent5");
        Assert.assertEquals("TEST_LOGS", actualLogs);
    }

    @Test
    public void testGetAgentById_checkLogs() {
        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("log", "TEST_LOGS");
        AgentHandle agent6 = AgentHandle.builder()
                .agentId("agent6")
                .agentType("satellite")
                .controllerNames(Set.of("myControllerTest"))
                .integrationIds(List.of("1", "2"))
                .telemetry(telemetry)
                .tenantId("ivan")
                .build();
        agentRegistryService.registerAgent(agent6);
        Optional<RegisteredAgent> agent = agentRegistryService.getAgentById("agent6");
        Assert.assertEquals("TEST_LOGS", agent.get().getAgentHandle().getTelemetry().get("log"));
    }

    @Test
    public void testGetAgentsByControllerName_checkLogs() {
        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("log", "testControllerLogs");
        AgentHandle agent6 = AgentHandle.builder()
                .agentId("agent7")
                .agentType("satellite")
                .controllerNames(Set.of("logControllerTest"))
                .integrationIds(List.of("1", "2"))
                .telemetry(telemetry)
                .tenantId("ivan")
                .build();
        agentRegistryService.registerAgent(agent6);
        List<RegisteredAgent> agents= agentRegistryService.getAgentsByControllerName("logControllerTest");
        Assert.assertEquals("testControllerLogs", agents.get(0).getAgentHandle().getTelemetry().get("log"));
    }

    @Test
    public void testRefreshHeartbeatAndTelemetry_checkLogsUpdate() {
        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("log", "TEST_LOGS");
        AgentHandle agent7 = AgentHandle.builder()
                .agentId("agent7")
                .agentType("satellite")
                .controllerNames(Set.of("myControllerTest"))
                .integrationIds(List.of("1", "2"))
                .telemetry(telemetry)
                .tenantId("ivan")
                .build();
        agentRegistryService.registerAgent(agent7);

        telemetry.put("log", "UPDATED_TEST_LOGS");
        agentRegistryService.refreshHeartbeatAndTelemetry(agent7);
        Optional<RegisteredAgent> agent = agentRegistryService.getAgentById("agent7");
        Assert.assertEquals("UPDATED_TEST_LOGS", agent.get().getAgentHandle().getTelemetry().get("log"));
    }

    @Test
    public void testClearAll() {
        agentRegistryService.clearAll();
        Jedis redis = (Jedis) redisFactory.getConnection().getNativeConnection();
        Assert.assertTrue(redis.hgetAll("ingestion_logs_agent").isEmpty());
        Assert.assertTrue(redis.hgetAll("cp_ar_by_id").isEmpty());
        Assert.assertTrue(redis.hgetAll("cp_ar_by_controller_myController").isEmpty());
        Assert.assertTrue(redis.hgetAll("cp_ar_by_controller_myControllerTest").isEmpty());
        Assert.assertTrue(redis.hgetAll("cp_ar_by_controller_myController2").isEmpty());
    }






}
