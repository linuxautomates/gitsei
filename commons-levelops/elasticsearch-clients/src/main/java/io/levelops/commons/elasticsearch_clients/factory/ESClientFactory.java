package io.levelops.commons.elasticsearch_clients.factory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Log4j2
@Value
@Builder(toBuilder = true)
@Service
public class ESClientFactory {
    private static int CONNECTION_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(1l);
    private static int READ_WRITE_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(5l);
    private static int MAX_CONNECTIONS_PER_ROUTE = 100;
    private static int MAX_CONNECTIONS_TOTAL = 4 * MAX_CONNECTIONS_PER_ROUTE;

    private ESClusterInfo defaultESClusterInfo;
    private Map<String, ESClusterInfo> tenantESClusterInfoMap;
    private LoadingCache<ESClusterInfo, ESContext> clusterInfoToContextCache;
    @Autowired
    public ESClientFactory(final List<ESClusterInfo> esClusterInfos) throws GeneralSecurityException, IOException {
        tenantESClusterInfoMap = new HashMap<>();
        clusterInfoToContextCache = CacheBuilder.newBuilder()
                .maximumSize(esClusterInfos.size())
                .build(ClusterInfoToContextCacheLoader.builder().build());

        ESClusterInfo defaultESClusterInfoLocal = null;

        for(ESClusterInfo esClusterInfo : esClusterInfos) {
            //Map companies to esClusterInfo
            CollectionUtils.emptyIfNull(esClusterInfo.getTenants())
                    .forEach(c -> tenantESClusterInfoMap.put(c, esClusterInfo));

            //Set Default Client
            if(esClusterInfo.isDefaultCluster()) {
                defaultESClusterInfoLocal = esClusterInfo;
            }
        }
        defaultESClusterInfo = defaultESClusterInfoLocal;
    }

    public ESClientFactory(ESClusterInfo defaultESClusterInfo, Map<String, ESClusterInfo> tenantESClusterInfoMap, LoadingCache<ESClusterInfo, ESContext> clusterInfoToContextCache) {
        this.defaultESClusterInfo = defaultESClusterInfo;
        this.tenantESClusterInfoMap = tenantESClusterInfoMap;
        this.clusterInfoToContextCache = clusterInfoToContextCache;
    }

    public ElasticsearchClient getESClient(final String company) {
        ESClusterInfo esClusterInfo = tenantESClusterInfoMap.getOrDefault(company, defaultESClusterInfo);
        log.info("getESClient company = {}, cluster_name = {}", company, esClusterInfo.getName());
        ESContext esContext = null;
        try {
            esContext = clusterInfoToContextCache.get(esClusterInfo);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to get ESContext from cache for company " + company, e);
        }
        //Check if rest client is running
        boolean restClientRunning = esContext.getRestClient().isRunning();
        log.info("getESClient first attempt company = {}, restClientRunning= {}", company, restClientRunning);
        if(restClientRunning) {
            return esContext.getClient();
        }

        //If closed invalidate cache
        clusterInfoToContextCache.invalidate(esClusterInfo);

        //Create ESContext again using CacheBuilder
        try {
            esContext = clusterInfoToContextCache.get(esClusterInfo);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to get ESContext from cache for company " + company, e);
        }

        restClientRunning = esContext.getRestClient().isRunning();
        log.info("getESClient second attempt company = {}, restClientRunning= {}", company, restClientRunning);
        if(!restClientRunning) {
            //If closed invalidate cache
            clusterInfoToContextCache.invalidate(esClusterInfo);
            throw new RuntimeException("Failed to build new ESContext for company " + company);
        }

        return esContext.getClient();
    }
}
