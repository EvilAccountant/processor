//package com.ming.processor.config;
//
//import com.mongodb.ConnectionString;
//import com.mongodb.MongoClientSettings;
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//import com.mongodb.client.MongoDatabase;
//import org.bson.codecs.configuration.CodecRegistries;
//import org.bson.codecs.configuration.CodecRegistry;
//import org.bson.codecs.pojo.PojoCodecProvider;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MongoConfig {
//
//    @Bean
//    public CodecRegistry codecRegistry() {
//        return CodecRegistries.fromRegistries(com.mongodb.MongoClient.getDefaultCodecRegistry(),
//                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
//    }
//
//    @Bean
//    public MongoClient mongoClient(CodecRegistry codecRegistry) {
//        MongoClientSettings settings = MongoClientSettings
//                .builder()
//                .codecRegistry(codecRegistry)
//                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:27017"))
//                .build();
//        return MongoClients.create(settings);
//    }
//
//    @Bean
//    public MongoDatabase mongoDatabase(MongoClient client, CodecRegistry registry) {
//        MongoDatabase database = client.getDatabase("processor");
//        return database.withCodecRegistry(registry);
//    }
//
//    @Bean
//    public MongoOperations mongoOperations(MongoDatabase database) {
//        return new MongoOperations(database);
//    }
//
//}
