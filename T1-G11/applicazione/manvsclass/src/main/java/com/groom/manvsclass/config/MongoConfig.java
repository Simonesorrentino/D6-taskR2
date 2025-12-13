package com.groom.manvsclass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableMongoRepositories(
        basePackages = "com.groom.manvsclass.model.repository.mongo"
)
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:nome_del_tuo_database_mongo}")
    private String databaseName;

    // --- 1. Mongo Client ---
    // Crea il client di connessione base.
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    // --- 2. Mongo Database Factory ---
    // Definisce l'accesso al database.
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }

    // --- 3. Mongo Mapping Context (Necessario per il Converter) ---
    // Definisce come le classi Java vengono mappate ai documenti Mongo.
    @Bean
    public MongoMappingContext mongoMappingContext() {
        return new MongoMappingContext();
    }

    // --- 4. Mongo Converter (IL BEAN MANCANTE) ---
    // Definito manualmente per risolvere l'errore NoSuchBeanDefinitionException.
    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory mongoDatabaseFactory,
            MongoMappingContext mongoMappingContext) {

        MappingMongoConverter converter = new MappingMongoConverter(mongoDatabaseFactory, mongoMappingContext);

        // Opzionale ma utile: rimuove il campo '_class' dai documenti MongoDB
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return converter;
    }

    // --- 5. Mongo Template ---
    // Il Template usa il Factory e il Converter che abbiamo definito.
    @Bean
    public MongoTemplate mongoTemplate(
            MongoDatabaseFactory mongoDatabaseFactory,
            MappingMongoConverter mappingMongoConverter) {

        return new MongoTemplate(mongoDatabaseFactory, mappingMongoConverter);
    }
}