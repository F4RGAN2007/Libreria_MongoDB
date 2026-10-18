package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends MongoRepository<Favorite, String> {

    Optional<Favorite> findByUserId(String userId);

    @Query("{ 'books.bookId': ?0 }")
    List<Favorite> findByBooksBookId(String bookId);

}
