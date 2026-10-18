package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Author;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthorRepository extends MongoRepository<Author, String> {


}
