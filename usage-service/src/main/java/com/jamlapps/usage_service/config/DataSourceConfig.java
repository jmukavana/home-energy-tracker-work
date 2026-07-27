package com.jamlapps.usage_service.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Boot's {@code DataSourceAutoConfiguration} is {@code @ConditionalOnMissingBean(DataSource.class)},
 * so adding the Timescale datasource suppresses the properties-driven MySQL one entirely —
 * both must be defined explicitly here. {@code mysqlDataSource} stays {@code @Primary} so
 * Boot's existing Flyway/JPA autoconfiguration keeps targeting MySQL unchanged.
 *
 * <p>Each datasource is built via a {@link DataSourceProperties} bean rather than binding
 * {@code @ConfigurationProperties} directly onto {@code DataSourceBuilder.create().build()}'s
 * result — the latter binds generic property names (e.g. {@code url}) via reflection on the
 * already-constructed concrete type (HikariDataSource, which only exposes {@code setJdbcUrl},
 * not {@code setUrl}), so {@code url} silently fails to bind. Going through
 * {@code DataSourceProperties.initializeDataSourceBuilder()} performs that url→jdbcUrl
 * translation correctly for whichever implementation is selected.
 *
 * <p>The two {@code DataSourceProperties} beans are consumed via direct Java method calls,
 * not method-parameter injection: with {@code mysqlDataSourceProperties} marked
 * {@code @Primary}, Spring's autowiring-by-type would resolve the {@code @Primary} bean for
 * both {@code DataSource} methods' parameters regardless of name, silently handing the
 * Timescale datasource the MySQL connection properties.
 *
 * <p>{@code timescaleDataSource} connects as the {@code postgres} superuser and is used
 * ONLY by {@link TimescaleMigrationRunner} for DDL — superusers bypass row-level security,
 * so the application's own reads/writes go through {@code timescaleAppDataSource} instead,
 * which connects as the restricted {@code usage_app} role created in
 * {@code db/timescale/V2__app_role.sql} and is therefore actually subject to the RLS
 * policy on {@code energy_usage}.
 *
 * <p>{@code mysqlDataSource} also accepts an optional {@link JdbcConnectionDetails}:
 * Boot's real {@code DataSourceAutoConfiguration} (which we bypass by defining this bean
 * ourselves) is what normally lets {@code @ServiceConnection}-based Testcontainers MySQL
 * override the static {@code spring.datasource.url}. Without this, test classes using
 * {@code MySqlTestcontainersBase} would silently fall back to the properties file's real
 * dev MySQL URL instead of the ephemeral test container — confirmed the hard way when
 * {@code DeviceUsageSummaryRepositoryTest} started reading/writing the live dev database.
 * Only {@code mysqlDataSource} does this; the Timescale beans below deliberately don't,
 * since there is no Postgres {@code @ServiceConnection} in this test suite and a single
 * {@link JdbcConnectionDetails} bean can't be told apart by target database.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource mysqlDataSource(ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
        JdbcConnectionDetails connectionDetails = connectionDetailsProvider.getIfAvailable();
        var builder = mysqlDataSourceProperties().initializeDataSourceBuilder();
        if (connectionDetails != null) {
            builder.url(connectionDetails.getJdbcUrl())
                    .username(connectionDetails.getUsername())
                    .password(connectionDetails.getPassword())
                    .driverClassName(connectionDetails.getDriverClassName());
        }
        return builder.build();
    }

    @Bean
    @ConfigurationProperties("timescale.datasource")
    public DataSourceProperties timescaleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource timescaleDataSource() {
        return timescaleDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("timescale.app-datasource")
    public DataSourceProperties timescaleAppDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource timescaleAppDataSource() {
        return timescaleAppDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate timescaleAppJdbcTemplate() {
        return new JdbcTemplate(timescaleAppDataSource());
    }
}
