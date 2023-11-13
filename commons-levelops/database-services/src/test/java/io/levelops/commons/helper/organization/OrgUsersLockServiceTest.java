package io.levelops.commons.helper.organization;

import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import redis.embedded.RedisServer;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class OrgUsersLockServiceTest {

    private static JedisConnectionFactory redisConnectionFactory1;
    private static JedisConnectionFactory redisConnectionFactory2;
    private static RedisServer redisServer1;
    private static OrgUsersLockService orgUsersLockService1;
    private static OrgUsersLockService orgUsersLockService2;
    private static RedisLockRegistry redisLockRegistry1;
    private static RedisLockRegistry redisLockRegistry2;

    @BeforeClass
    public static void setup() throws SQLException {
        redisServer1 = new RedisServer(3743);
        redisServer1.start();
        RedisStandaloneConfiguration config1 = new RedisStandaloneConfiguration("localhost", 3743);
        RedisStandaloneConfiguration config2 = new RedisStandaloneConfiguration("localhost", 3743);
        redisConnectionFactory1 = new JedisConnectionFactory(config1);
        redisConnectionFactory1.afterPropertiesSet();
        redisConnectionFactory2 = new JedisConnectionFactory(config2);
        redisConnectionFactory2.afterPropertiesSet();
    }

    @Before
    public void resetRedis() {
        redisConnectionFactory1.getConnection().flushAll();
    }

    @Test
    public void testLock() throws InterruptedException {
        orgUsersLockService1 = new OrgUsersLockService(redisConnectionFactory1);
        orgUsersLockService2 = new OrgUsersLockService(redisConnectionFactory2);
        var locked1 = orgUsersLockService1.lock("test", 10);
        assertThat(locked1).isTrue();

        var locked2 = orgUsersLockService2.lock("test", 1);
        assertThat(locked2).isFalse();

        // After unlocking the first one, the second one should be able to lock
        orgUsersLockService1.unlock("test");
        locked2 = orgUsersLockService2.lock("test", 1);
        assertThat(locked2).isTrue();
    }

    @Test
    public void testTimeout() throws InterruptedException {
        orgUsersLockService1 = new OrgUsersLockService(redisConnectionFactory1, 3000);
        orgUsersLockService2 = new OrgUsersLockService(redisConnectionFactory2, 3000);
        var locked1 = orgUsersLockService1.lock("test", 1);
        assertThat(locked1).isTrue();

        // default timeout is set to 3 seconds, so waiting for 3 seconds should be enough
        var locked2 = orgUsersLockService2.lock("test", 3);
        assertThat(locked2).isTrue();

        // lock1 should have expired by now, and unlocking an expired lock should be an exception
        assertThatThrownBy(() -> orgUsersLockService1.unlock("test"))
                .isInstanceOf(IllegalStateException.class);

        var unlocked = orgUsersLockService2.unlock("test");
        assertThat(unlocked).isTrue();
    }


    @Test
    public void testLockInDifferentThreads() throws InterruptedException, ExecutionException {
        orgUsersLockService1 = new OrgUsersLockService(redisConnectionFactory1);
        orgUsersLockService2 = new OrgUsersLockService(redisConnectionFactory2);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        var a = executor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var locked1 = orgUsersLockService1.lock("test", 10);
            assertThat(locked1).isTrue();
        });

        var b = executor.submit(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var locked1 = orgUsersLockService2.lock("test", 1);
            assertThat(locked1).isFalse();
        });
        System.out.println("Submitted both");
        a.get();
        b.get();
    }
}