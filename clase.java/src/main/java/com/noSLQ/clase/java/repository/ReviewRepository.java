package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByBookId(String bookId);
    List<Review> findByUserId(String userId);
    List<Review> findByBookIdOrderByCreatedAtDesc(String bookId);
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndBookId(String userId, String bookId);
    long countByBookId(String bookId);

    void deleteByBookId(String bookId);
    void deleteByUserId(String userId);

}
