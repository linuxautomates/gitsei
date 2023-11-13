package io.levelops.commons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import java.util.concurrent.TimeUnit;

@Configuration
public class CommonConfig {
    private static final Log LOGGER = LogFactory.getLog(CommonConfig.class);
    private static final Long DB_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    private static final Long DB_LEAK_DETECTION_THRESHOLD = TimeUnit.MINUTES.toMillis(2);

    @Value("${DB_NAME:postgres}")
    private String DATABASE_NAME;
    @Value("${DB_IP}")
    private String DATABASE_IP;
    @Value("${DB_USERNAME}")
    private String DATABASE_USERNAME;
    @Value("${DB_PASSWORD}")
    private String DATABASE_PASSWORD;
    @Value("${DB_SSL_MODE:require}")
    private String SSL_MODE;
    @Value("${DB_SSL_ROOT_CERT}")
    private String serverCert;
    @Value("${DB_SSL_KEY}")
    private String clientKey;
    @Value("${DB_SSL_PASSWORD}")
    private String clientKeyPassword;
    @Value("${DB_SSL_CERT}")
    private String clientCert;
    @Value("${DB_MAX_POOL_SIZE:10}")
    private Integer maxPoolSize;

    @Bean(name = "custom")
    @Primary
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = "jdbc:postgresql://" + DATABASE_IP + "/postgres?";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(DATABASE_USERNAME);
        config.setPassword(DATABASE_PASSWORD);
        config.addDataSourceProperty(PGProperty.SSL_MODE.getName(), SSL_MODE);
        config.addDataSourceProperty(PGProperty.SSL_CERT.getName(), clientCert);
        config.addDataSourceProperty(PGProperty.SSL_KEY.getName(), clientKey);
        config.addDataSourceProperty(PGProperty.SSL_PASSWORD.getName(), clientKeyPassword);
        config.addDataSourceProperty(PGProperty.SSL_ROOT_CERT.getName(), serverCert);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(DB_CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(DB_LEAK_DETECTION_THRESHOLD);
        return new HikariDataSource(config);
    }

    @Bean(name = "simple_data_source")
    public PGSimpleDataSource simpleDataSource() {
        PGSimpleDataSource pgSimpleDataSource = new PGSimpleDataSource();
        pgSimpleDataSource.setServerName(DATABASE_IP);
        pgSimpleDataSource.setUser(DATABASE_USERNAME);
        pgSimpleDataSource.setPassword(DATABASE_PASSWORD);
        pgSimpleDataSource.setProperty(PGProperty.SSL_MODE, SSL_MODE);
        pgSimpleDataSource.setSslCert(clientCert);
        pgSimpleDataSource.setSslKey(clientKey);
        pgSimpleDataSource.setSslPassword(clientKeyPassword);
        pgSimpleDataSource.setSslRootCert(serverCert);
        pgSimpleDataSource.setDatabaseName(DATABASE_NAME);
        return pgSimpleDataSource;
    }
}
