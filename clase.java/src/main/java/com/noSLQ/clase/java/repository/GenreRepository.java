package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Genre;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenreRepository extends MongoRepository<Genre, String> {


}
