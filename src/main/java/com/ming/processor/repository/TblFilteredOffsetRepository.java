package com.ming.processor.repository;

import com.ming.processor.entity.TblFilteredOffset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TblFilteredOffsetRepository extends MongoRepository<TblFilteredOffset, String> {

    @Query("{'collectTimeValue' : {'$gte' : ?0, '$lte' : ?1 }}")
    List<TblFilteredOffset> findByCollectTimeValueBetweenOrderByCollectTimeValue(String headTime, String endTime);

}
