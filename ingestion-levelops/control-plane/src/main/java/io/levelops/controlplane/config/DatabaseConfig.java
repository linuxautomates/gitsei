package io.levelops.controlplane.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.postgresql.PGProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Log4j2
@Configuration
public class DatabaseConfig {

    private static final String JDBC_POSTGRESS_URL_FORMAT = "jdbc:postgresql://%s/%s?";

    @Bean("controlPlaneDatabaseSettings")
    public DatabaseSettings controlPlaneDatabaseSettings(
            @Value("${DB_IP}") String ip,
            @Value("${DB_NAME:${database.name}}") String dbName,
            @Value("${DB_USERNAME}") String userName,
            @Value("${DB_PASSWORD}") String password,
            @Value("${DB_SSL:true}") Boolean ssl) {

        return DatabaseSettings.builder()
                .url(String.format(JDBC_POSTGRESS_URL_FORMAT, ip, dbName))
                .ip(ip)
                .userName(userName)
                .password(password)
                .databaseName(dbName)
                .ssl(ssl)
                .build();
    }

    @Bean("controlPlaneDataSource")
    public DataSource controlPlaneDataSource(
        @Qualifier("controlPlaneDatabaseSettings") DatabaseSettings databaseSettings,
        @Value("${DB_SSL_MODE}") String sslMode,
        @Value("${DB_SSL_ROOT_CERT}") String serverCert,
        @Value("${DB_SSL_KEY}") String clientKey,
        @Value("${DB_SSL_PASSWORD}") String clientKeyPassword,
        @Value("${DB_SSL_CERT}") String clientCert) {
        log.info("Connecting as {} to {}/{}", databaseSettings.getUserName(), databaseSettings.getIp(), databaseSettings.getDatabaseName());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseSettings.getUrl());
        config.setUsername(databaseSettings.getUserName());
        config.setPassword(databaseSettings.getPassword());
        if (databaseSettings.isSsl()) {
            config.addDataSourceProperty(PGProperty.SSL_MODE.getName(), sslMode);
            config.addDataSourceProperty(PGProperty.SSL_CERT.getName(), clientCert);
            config.addDataSourceProperty(PGProperty.SSL_KEY.getName(), clientKey);
            config.addDataSourceProperty(PGProperty.SSL_PASSWORD.getName(), clientKeyPassword);
            config.addDataSourceProperty(PGProperty.SSL_ROOT_CERT.getName(), serverCert);
        }
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }

    @Bean("controlPlaneJdbcTemplate")
    public NamedParameterJdbcTemplate controlPlaneJdbcTemplate(@Qualifier("controlPlaneDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class DatabaseSettings {
        String url;
        String ip;
        String databaseName;
        String userName;
        String password;
        boolean ssl;

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("url", url)
                    .append("ip", ip)
                    .append("databaseName", databaseName)
                    .append("userName", userName)
                    .append("password", StringUtils.repeat("*", StringUtils.length(password)))
                    .append("ssl", ssl)
                    .toString();
        }
    }

}
