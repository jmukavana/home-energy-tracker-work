package com.jamlapps.usage_service.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Migrates the Timescale datasource via Flyway's plain Java API rather than a
 * {@code @Bean Flyway}, so it can't interfere with the ordering Boot wires up
 * for the MySQL {@code Flyway} bean (which JPA's EntityManagerFactory depends on).
 */
@Component
@Slf4j
public class TimescaleMigrationRunner implements ApplicationRunner {

    private final DataSource timescaleDataSource;

    public TimescaleMigrationRunner(@Qualifier("timescaleDataSource") DataSource timescaleDataSource) {
        this.timescaleDataSource = timescaleDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running Flyway migration against TimescaleDB");
        Flyway.configure()
                .dataSource(timescaleDataSource)
                .table("flyway_schema_history_usage_timescale")
                .locations("classpath:db/timescale")
                .load()
                .migrate();
    }
}
