package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {

    List<Book> findByCategory(String category);
    List<Book> findByAuthor(String author);
    List<Book> findByTitle(String title);
    List<Book> findByAvailable(boolean available);
    List<Book> findByBorrowed(boolean borrowed);
    List<Book> findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(double rating);
    List<Book> findAllByOrderByAverageRatingDesc();
    List<Book> findAllByOrderByTotalReviewsDesc();

    Optional<Book> findByIsbn(String isbn);

}
