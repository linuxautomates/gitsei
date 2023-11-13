package io.levelops.commons.helper.organization;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Log4j2
public class OrgUsersLockService {
    private final static String LOCK_PREFIX = "org-users-lock-registry";
    private final static int DEFAULT_LOCK_EXPIRE = 1000 * 60 * 5; // 5 minutes
    private final int lockTimeout;
    private final RedisLockRegistry redisLockRegistry;
    private final RedisConnectionFactory redisConnectionFactory;

    public OrgUsersLockService(RedisConnectionFactory redisConnectionFactory) {
        this(redisConnectionFactory, DEFAULT_LOCK_EXPIRE);
    }

    public OrgUsersLockService(RedisConnectionFactory redisConnectionFactory, int lockTimeout) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.lockTimeout = lockTimeout;
        redisLockRegistry = new RedisLockRegistry(redisConnectionFactory, LOCK_PREFIX, lockTimeout);
    }

    public boolean lock(String tenantId, int waitTimeoutSeconds) {
        redisLockRegistry.expireUnusedOlderThan(lockTimeout);
        var lock = redisLockRegistry.obtain(tenantId);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!locked) {
            log.warn("Failed to obtain org user lock for tenantId={}", tenantId);
        } else {
            log.info("Org User Service lock acquired for tenantId={}", tenantId);
        }
        return locked;
    }

    public boolean unlock(String tenantId) {
        Lock lock = redisLockRegistry.obtain(tenantId);
        if (lock == null) {
            log.warn("Failed to obtain org user lock for tenantId={}", tenantId);
            return false;
        }
        lock.unlock();
        log.info("Org User Service lock released for tenantId={}", tenantId);
        return true;
    }
}
