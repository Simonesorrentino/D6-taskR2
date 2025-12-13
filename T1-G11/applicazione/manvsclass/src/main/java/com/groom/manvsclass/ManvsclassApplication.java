package com.groom.manvsclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

//MODIFICA (13/02/2024) : Autenticazione token provenienti players
// import org.springframework.context.annotation.Bean;
// import org.springframework.web.client.RestTemplate;
//FINE MODIFICA

@SpringBootApplication(
        exclude = {
                // 1. Esclude l'auto-configurazione di Hikari/DataSource
                DataSourceAutoConfiguration.class,
                // 2. Esclude l'auto-configurazione di Hibernate/JPA
                HibernateJpaAutoConfiguration.class,
                // 3. Esclude l'auto-configurazione di Spring Data MongoDB
                MongoDataAutoConfiguration.class
        }
)
public class ManvsclassApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManvsclassApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
