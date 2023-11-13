package io.levelops.commons.services.business_alignment.es.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.elasticsearch_clients.factory.ClusterInfoToContextCacheLoader;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.util.Map;

public class EsTestUtils {
    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.2.0";

    private ElasticsearchContainer container;
    private RestClient restClient;
    private ElasticsearchClient elasticsearchClient;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    public ESContext initializeESClient(){

        // Create the elasticsearch container.
        container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE);
        container.addExposedPorts(9200, 9300);
        container.withPassword("s3cret");
        container.getEnvMap().remove("xpack.security.enabled");
        // custom wait strategy not requiring any TLS tuning...
        container.setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*\"message\":\"started\".*"));
        // Start the container. This step might take some time...
        container.start();

        byte[] certAsBytes = container.copyFileFromContainer("/usr/share/elasticsearch/config/certs/http_ca.crt", InputStream::readAllBytes);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "s3cret"));

        HttpHost host = new HttpHost("localhost", container.getMappedPort(9200), "https");
        final RestClientBuilder builder = RestClient.builder(host);
        builder.setHttpClientConfigCallback(clientBuilder -> {
            clientBuilder.setSSLContext(createContextFromCaCert(certAsBytes));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return clientBuilder;
        });

        // Create the low-level client
        restClient = builder.build();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(DefaultObjectMapper.get()));
        // And create the API client
        elasticsearchClient = new ElasticsearchClient(transport);
        return ESContext.builder()
                .restClient(restClient)
                .transport(transport)
                .client(elasticsearchClient)
                .build();
    }

    public static ESClientFactory buildESClientFactory(ESContext esContext) {
        ESClusterInfo esClusterInfo = ESClusterInfo.builder().name("CLUSTER-1").defaultCluster(true).build();
        LoadingCache<ESClusterInfo, ESContext> loadingCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(ClusterInfoToContextCacheLoader.builder().build());
        loadingCache.put(esClusterInfo, esContext);
        ESClientFactory esClientFactory = new ESClientFactory(esClusterInfo, Map.of(), loadingCache);
        return esClientFactory;
    }

    public void deleteIndex(String index) throws Exception {
        elasticsearchClient.indices().delete(b -> b.index(index));
    }

    public void closeResources() throws Exception {
        restClient.close();
        container.stop();
    }

    public static SSLContext createContextFromCaCert(byte[] certAsBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa = factory.generateCertificate(
                    new ByteArrayInputStream(certAsBytes)
            );
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder =
                    SSLContexts.custom().loadTrustMaterial(trustStore, null);
            return sslContextBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createIndex(ElasticsearchClient elasticsearchClient, String index, String indexPath ) throws IOException {

        String indexTemplate = ResourceUtils.getResourceAsString(indexPath);
        elasticsearchClient.indices().create(new CreateIndexRequest.Builder()
                .index(index)
                .aliases(index+"_a", new Alias.Builder().build())
                .settings(new IndexSettings.Builder().numberOfShards("4").codec("best_compression").build())
                .mappings(TypeMapping.of(s -> s.withJson(new StringReader(indexTemplate))))
                .build());
    }
    public static String insertIntegration(DataSource dataSource, Integration integration) throws SQLException, JsonProcessingException {

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        String SQL = "INSERT INTO " +  "test.integrations " +
                "(id,name,url,status,application,description,satellite, metadata, authentication) " +
                " VALUES " +
                "(:id, :name, :url, :status, :application, :description, :satellite, to_json(:metadata::jsonb), :authentication)";
        Map<String, Object> params = Map.of(
                "id", Integer.parseInt(integration.getId()),
                "name", integration.getName(),
                "url", StringUtils.defaultString(integration.getUrl()),
                "status", integration.getStatus(),
                "application", integration.getApplication(),
                "description", StringUtils.defaultString(integration.getDescription()),
                "satellite", BooleanUtils.toBooleanDefaultIfNull(integration.getSatellite(), false),
                "metadata", mapper.writeValueAsString(MapUtils.emptyIfNull(integration.getMetadata())),
                "authentication", MoreObjects.firstNonNull(integration.getAuthentication(), Integration.Authentication.UNKNOWN).toString());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(SQL, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
}
