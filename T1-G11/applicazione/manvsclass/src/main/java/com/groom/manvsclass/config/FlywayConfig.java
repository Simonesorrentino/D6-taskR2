package com.groom.manvsclass.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    // Flyway ha bisogno del DataSource per connettersi al DB
    private final DataSource dataSource;

    public FlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                // Percorso dove si trova il file V1__initial_schema.sql
                .locations("classpath:db/migrations")
                // RUCIALE per lo schema preesistente
                .baselineOnMigrate(true)
                // Se usi uno schema diverso da 'public', specificarlo qui:
                // .schemas("tuo_schema_db")

                // Opzionale: Specificare la versione iniziale per la baseline,
                // in base al tuo file V1.
                .baselineVersion("1")

                .load();
    }
}