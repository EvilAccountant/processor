//package com.ming.processor.config;
//
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Consumer;
//
//public class MongoOperations {
//
//    private MongoDatabase database;
//
//    public MongoOperations(MongoDatabase database) {
//        this.database = database;
//    }
//
//    public <T> List<T> findAll(Class<T> clazz) {
//        MongoCollection<T> collection = getCollection(clazz);
//        List<T> list = new ArrayList<>();
//        collection.find().forEach((Consumer<T>)list::add);
//        return list;
//    }
//
//    private <T> MongoCollection<T> getCollection(Class<T> clazz) {
//        if (!clazz.isAnnotationPresent(Document.class)) throw new IllegalArgumentException("asdasda");
//        Document doc = clazz.getAnnotation(Document.class);
//        return database.getCollection(doc.value(), clazz);
//    }
//
//
//}