package io.levelops.commons.elasticsearch_clients.factory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.common.cache.CacheLoader;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class ClusterInfoToContextCacheLoader extends CacheLoader<ESClusterInfo, ESContext> {
    private static int CONNECTION_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(1l);
    private static int READ_WRITE_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(5l);
    private static int MAX_CONNECTIONS_PER_ROUTE = 100;
    private static int MAX_CONNECTIONS_TOTAL = 4 * MAX_CONNECTIONS_PER_ROUTE;

    private CredentialsProvider processBasicAuth(ESClusterInfo esClusterInfo) {
        if(StringUtils.isBlank(esClusterInfo.getUserName())) {
            return null;
        }
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esClusterInfo.getUserName(), esClusterInfo.getPassword()));
        return credentialsProvider;
    }

    private SSLContext processSSLContext(ESClusterInfo esClusterInfo) throws GeneralSecurityException, IOException {
        if(StringUtils.isBlank(esClusterInfo.getSslCertPath())) {
            return null;
        }
        Path caCertificatePath = Paths.get(esClusterInfo.getSslCertPath());
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate trustedCa;
        try (InputStream is = Files.newInputStream(caCertificatePath)) {
            trustedCa = factory.generateCertificate(is);
        }
        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);
        SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);
        SSLContext sslContext = sslContextBuilder.build();
        return sslContext;
    }

    @Override
    public ESContext load(ESClusterInfo esClusterInfo) throws Exception {
        //Set ES SSL
        final SSLContext sslContext = processSSLContext(esClusterInfo);

        List<HttpHost> httpHosts = CollectionUtils.emptyIfNull(esClusterInfo.getIpAddresses()).stream()
                .distinct()
                .map(ip -> (sslContext != null) ? new HttpHost(ip, esClusterInfo.getPort(), "https") : new HttpHost(ip, esClusterInfo.getPort()))
                .collect(Collectors.toList());

        // Create the low-level client
        //Create builder
        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));

        //Overwrite Connection & Read-Write Timeout
        builder.setRequestConfigCallback(r -> r
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_WRITE_TIMEOUT));

        //Set Username Password credentials
        final CredentialsProvider credentialsProvider = processBasicAuth(esClusterInfo);

        //Set Basic Auth & SSL
        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                if(credentialsProvider != null) {
                    httpClientBuilder = httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
                if(sslContext != null) {
                    httpClientBuilder = httpClientBuilder.setSSLContext(sslContext);
                }

                //Overwrite Connection Pool Values
                httpClientBuilder.setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE);
                httpClientBuilder.setMaxConnTotal(MAX_CONNECTIONS_TOTAL);

                return httpClientBuilder;
            }
        });

        RestClient restClient = builder.build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        // And create the API client
        ElasticsearchClient client = new ElasticsearchClient(transport);

        ESContext esContext = ESContext.builder()
                .restClient(restClient)
                .transport(transport)
                .client(client)
                .build();
        return esContext;
    }
}
