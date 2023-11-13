package io.levelops.commons.elasticsearch_clients.config;

import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2
@Configuration
public class ESClusterConfig {
    private static final String ES_CLUSTER_IPS_FORMAT = "ES_CLUSTER_IPS_";
    private static final String ES_CLUSTER_PORT_FORMAT = "ES_CLUSTER_PORT_";
    private static final String ES_CLUSTER_USERNAME_FORMAT = "ES_CLUSTER_USERNAME_";
    private static final String ES_CLUSTER_PASSWORD_FORMAT = "ES_CLUSTER_PASSWORD_";
    private static final String ES_CLUSTER_SSL_CERT_FORMAT = "ES_CLUSTER_SSL_CERT_";
    private static final String ES_CLUSTER_TENANTS_FORMAT = "ES_CLUSTER_TENANTS_";

    /*
    Faceted Search service expects the following.
    1) Cluster Names (Optional, Default CLUSTER_1)
    ES_CLUSTERS=CLUSTER_1,CLUSTER_2

    2) For each cluster, IPs, Port(Optional, Default 9200), Username(Optional, Default null), Password(Optional, Default null), SSL Cert Path (Optional, Default null) & Tenants(Optional, Default null or empty)
    ES_CLUSTER_IPS_CLUSTER_1=10.128.15.195,10.128.15.196
    ES_CLUSTER_PORT_CLUSTER_1=9200
    ES_CLUSTER_USERNAME_CLUSTER_1=uname1
    ES_CLUSTER_PASSWORD_CLUSTER_1=pass1234
    ES_CLUSTER_SSL_CERT_CLUSTER_1=/etc/es-ssl/cluster1/ca.crt
    ES_CLUSTER_TENANTS_CLUSTER_1 => does NOT exist

    ES_CLUSTER_IPS_CLUSTER_2=10.128.15.195,10.128.15.196
    ES_CLUSTER_PORT_CLUSTER_2=9200
    ES_CLUSTER_TENANTS_CLUSTER_2 => pepsi,cocacola

    3) Default Cluster name (Optional, Default CLUSTER_1)
    ES_DEFAULT_CLUSTER=CLUSTER_1
     */

    @Bean("esClusterNames")
    public List<String> esClusterNames(@Value("${ES_CLUSTERS:CLUSTER_1}") String esClusters) {
        if (StringUtils.isBlank(esClusters)) {
            return Collections.EMPTY_LIST;
        }
        List<String> esClustersList = Arrays.asList(esClusters.split(","));
        log.info("esClusters = {}", esClustersList);
        return esClustersList;
    }

    @Bean
    public List<ESClusterInfo> esClusterInfos(@Qualifier("esClusterNames") List<String> esClusterNames, @Value("${ES_DEFAULT_CLUSTER:CLUSTER_1}") String defaultClusterName) {
        log.info("esClusterNames = {}", esClusterNames);
        List<ESClusterInfo> esClusterInfos = new ArrayList<>();
        for(String clusterName : esClusterNames) {
            if(StringUtils.isBlank(clusterName)) {
                continue;
            }

            boolean isDefault = clusterName.equalsIgnoreCase(defaultClusterName);
            log.info("clusterName = {}, isDefault = {}", clusterName, isDefault);

            String ipsKey = ES_CLUSTER_IPS_FORMAT + clusterName;
            String ips = System.getenv().getOrDefault(ipsKey, "");
            log.info("clusterName = {}, ips = {}", clusterName, ips);

            if(StringUtils.isBlank(ips)) {
                continue;
            }

            String portsKey = ES_CLUSTER_PORT_FORMAT + clusterName;
            String portString = System.getenv().getOrDefault(portsKey, "9200");

            String userNameKey = ES_CLUSTER_USERNAME_FORMAT + clusterName;
            String userName = System.getenv().get(userNameKey);

            String passwordKey = ES_CLUSTER_PASSWORD_FORMAT + clusterName;
            String password = System.getenv().get(passwordKey);

            String sslCertKey = ES_CLUSTER_SSL_CERT_FORMAT + clusterName;
            String sslCertPath = System.getenv().get(sslCertKey);

            String tenantsKey = ES_CLUSTER_TENANTS_FORMAT + clusterName;
            String tenants = System.getenv().getOrDefault(tenantsKey, "");

            ESClusterInfo esClusterInfo = ESClusterInfo.builder()
                    .name(clusterName)
                    .ipAddresses(Arrays.asList(ips.split(",")))
                    .port(Integer.valueOf(portString))
                    .userName(userName)
                    .password(password)
                    .sslCertPath(sslCertPath)
                    .defaultCluster(isDefault)
                    .tenants(Arrays.asList(tenants.split(",")))
                    .build();

            //log.info("esClusterInfo = {}", esClusterInfo);
            esClusterInfos.add(esClusterInfo);
        }
        //log.info("esClusterInfos = {}", esClusterInfos);
        return esClusterInfos;
    }
}
