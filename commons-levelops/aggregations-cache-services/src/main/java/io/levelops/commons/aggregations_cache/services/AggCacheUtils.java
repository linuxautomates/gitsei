package io.levelops.commons.aggregations_cache.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.exceptions.JedisException;

@Log4j2
public class AggCacheUtils {

    public static <T> DbListResponse<T> cacheOrCall(Boolean disableCache, String company, String misc, String cacheHash,
                                                    List<String> integrationIds, ObjectMapper mapper, AggCacheService cacheService,
                                                    Callable<DbListResponse<T>> callable) throws Exception {
        DbListResponse<T> resp = null;
        if (Boolean.TRUE.equals(disableCache))
            return callable.call();
        Optional<String> data = Optional.empty();
        try {
            data = cacheService.getQueryData(company, misc, integrationIds, cacheHash);
        } catch (Exception e) {
            if (!(e instanceof JedisException)) {
                log.debug("failed to use cache due to jedis error.");
            }
            log.warn("failed to use cache due to unknown error.", e);
        }
        if (data.isPresent())
            resp = mapper.readValue(data.get(), new TypeReference<>() {
            });
        else {
            resp = callable.call();
            try {
                cacheService.saveQueryData(company, misc, integrationIds,
                        cacheHash, mapper.writeValueAsString(resp));
            } catch (Exception e) {
                if (!(e instanceof JedisException)) {
                    log.debug("failed to save in cache due to jedis error.");
                }
                log.warn("failed to save in cache due to unknown error.", e);
            }
        }
        return resp;
    }

    public static <T> T cacheOrCallGeneric(Boolean disableCache, String company, String misc, String cacheHash,
                                           List<String> integrationIds, ObjectMapper mapper, AggCacheService cacheService,
                                           Class clazz, Long cacheTTLValue, TimeUnit cacheTTLTimeUnit,
                                           Callable<T> callable) throws Exception {
        T resp = null;
        if (Boolean.TRUE.equals(disableCache))
            return callable.call();
        Optional<String> data = Optional.empty();
        try {
            data = cacheService.getQueryData(company, misc, integrationIds, cacheHash);
        } catch (Exception e) {
            if (!(e instanceof JedisException)) {
                log.debug("failed to use cache due to jedis error.");
            }
            log.warn("failed to use cache due to unknown error.", e);
        }

        if (data.isPresent()) {
            log.info("data present in cache");
            if(clazz != null)
                resp = (T) mapper.readValue(data.get(), clazz);
            else
                resp = mapper.readValue(data.get(), new TypeReference<>() {});

        } else {
            resp = callable.call();
            try {
                if ((cacheTTLValue != null) && (cacheTTLTimeUnit != null)) {
                    cacheService.saveQueryData(company, misc, integrationIds, cacheHash, mapper.writeValueAsString(resp), cacheTTLValue, cacheTTLTimeUnit);
                } else {
                    cacheService.saveQueryData(company, misc, integrationIds, cacheHash, mapper.writeValueAsString(resp));
                }
            } catch (Exception e) {
                if (!(e instanceof JedisException)) {
                    log.debug("failed to save in cache due to jedis error.");
                }
                log.warn("failed to save in cache due to unknown error.", e);
            }
        }
        return resp;
    }
}
