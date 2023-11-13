package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.agent.config.SatelliteConfigFileProperties.SatelliteProperties.ProxySettings;
import io.levelops.ingestion.agent.utils.ProxyUtils;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
public class BaseConfig {

    @Bean
    @Primary
    public OkHttpClient okHttpClient(@Qualifier("nonProxyOkHttpClient") OkHttpClient nonProxyOkHttpClient,
                                     @Qualifier("proxyOkHttpClient") OkHttpClient proxyOkHttpClient,
                                     SatelliteConfigFileProperties properties) {
        if (properties.getSatellite().getProxy().getAllTraffic()) {
            // use the proxy client for integrations only if all traffic is enabled
            log.info("Proxying all traffic enabled");
            return proxyOkHttpClient;
        } else {
            return nonProxyOkHttpClient;
        }
    }

    @Bean("nonProxyOkHttpClient")
    public OkHttpClient nonProxyOkHttpClient(SatelliteConfigFileProperties properties) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.MINUTES)
                .connectTimeout(2, TimeUnit.MINUTES);
        return builder.build();
    }

    /**
     * Returns a client with proxy setup IF requested in the config file. Otherwise, it is the same as nonProxyOkHttpClient.
     * The control plane connection always uses this client.
     */
    @Bean("proxyOkHttpClient")
    public OkHttpClient proxyOkHttpClient(@Qualifier("nonProxyOkHttpClient") OkHttpClient nonProxyOkHttpClient,
                                          SatelliteConfigFileProperties properties) {
        OkHttpClient.Builder builder = nonProxyOkHttpClient.newBuilder();
        ProxySettings proxy = properties.getSatellite().getProxy();
        if (StringUtils.isNotEmpty(proxy.getHost())) {
            Proxy.Type proxyType = ProxyUtils.parseProxyType(proxy.getType());
            log.info("Proxy enabled: type={} host={} port={} all_traffic={}", proxyType.toString(), proxy.getHost(), proxy.getPort(), proxy.getAllTraffic());
            builder.proxy(ProxyUtils.buildProxy(proxyType, proxy.getHost(), proxy.getPort()));
        }
        if (StringUtils.isNotEmpty(proxy.getUsername())) {
            Validate.notBlank(proxy.getPassword(), "proxyPassword cannot be null or empty.");
            log.info("Proxy authenticated for user={}", proxy.getUsername());
            builder.proxyAuthenticator(ProxyUtils.buildProxyAuthenticator(proxy.getAuthorizationHeader(), proxy.getUsername(), proxy.getPassword()));
        }
        return builder.build();
    }

}
