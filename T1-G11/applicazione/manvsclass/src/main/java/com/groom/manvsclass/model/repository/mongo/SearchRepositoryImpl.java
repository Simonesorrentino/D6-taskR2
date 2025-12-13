package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.interactionMongoDB;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Component
public class SearchRepositoryImpl {

    @Autowired
    MongoClient client;

    @Autowired
    MongoConverter converter;

    public long getLikes(String name) {
        MongoDatabase database = client.getDatabase("manvsclass");
        MongoCollection<Document> collection = database.getCollection("interaction");

        Bson textSearch = Filters.text(name);
        Bson typeFilter = Filters.eq("type", 1);

        long count = collection.countDocuments(Filters.and(textSearch, typeFilter));

        return count;
    }

    public List<interactionMongoDB> findReport() {

        final List<interactionMongoDB> posts = new ArrayList<>();

        MongoDatabase database = client.getDatabase("manvsclass");
        MongoCollection<Document> collection = database.getCollection("interaction");

        AggregateIterable<Document> result = collection.aggregate(
                Arrays.asList(
                        new Document("$match",
                                new Document("type", 0)
                        )
                )
        );

        result.forEach(doc -> posts.add(converter.read(interactionMongoDB.class, doc)));

        return posts;
    }

}
