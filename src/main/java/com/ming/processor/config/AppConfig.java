//package com.ming.processor.config;
//
//import com.mongodb.MongoClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.core.MongoTemplate;
//
//@Configuration
//public class AppConfig {
//
//    public @Bean
//    MongoClient mongo() throws Exception {
//        return new MongoClient("localhost");
//    }
//
//    public @Bean
//    MongoTemplate mongoTemplate() throws Exception {
//        return new MongoTemplate(mongo(), "processor");
//    }
//}