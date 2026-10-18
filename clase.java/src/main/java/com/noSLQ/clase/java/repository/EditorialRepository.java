package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Editorial;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EditorialRepository extends MongoRepository<Editorial, String> {


}
