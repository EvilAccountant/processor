package com.ming.processor.repository;

import com.ming.processor.entity.TblOriginOffset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TblOriginOffsetRepository extends MongoRepository<TblOriginOffset, String> {

    @Query("{'collectTimeValue' : {'$gte' : ?0, '$lte' : ?1 }}")
    List<TblOriginOffset> findByCollectTimeValueBetweenOrderByCollectTimeValue(String headTime, String endTime);

}
