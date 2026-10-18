package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.ReviewRequest;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.BookSummary;
import com.noSLQ.clase.java.entity.Review;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.entity.UserSummary;
import com.noSLQ.clase.java.repository.BookRepository;
import com.noSLQ.clase.java.repository.ReviewRepository;
import com.noSLQ.clase.java.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@AllArgsConstructor
@Service
public class ReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Cacheable(cacheNames = CacheNames.REVIEWS)
    public List<Review> getAll(){
        return reviewRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.REVIEWS, key = "'id:' + #id")
    public Review getById(String id){
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
    }

    @Cacheable(cacheNames = CacheNames.REVIEWS, key = "'book:' + #bookId")
    public List<Review> getByBook(String bookId){
        return reviewRepository.findByBookId(bookId);
    }

    @Cacheable(cacheNames = CacheNames.REVIEWS, key = "'user:' + #userId")
    public List<Review> getByUser(String userId){
        return reviewRepository.findByUserId(userId);
    }

    @CacheEvict(cacheNames = {
            CacheNames.REVIEWS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED
    }, allEntries = true)
    public Review createReview(ReviewRequest request){
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + request.getBookId()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        validateRating(request.getRating());

        if (reviewRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
            throw new RuntimeException("User already reviewed book with id: " + book.getId());
        }

        Review review = new Review();
        int rating = request.getRating();
        review.setUserId(user.getId());
        review.setBookId(book.getId());
        review.setRating(rating);
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());
        review.setBook(buildBookSummary(book));
        review.setUser(buildUserSummary(user));

        Review saved = reviewRepository.save(review);

        refreshBookRating(book.getId());

        return saved;
    }

    @CacheEvict(cacheNames = {
            CacheNames.REVIEWS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED
    }, allEntries = true)
    public Review updateReview(String id, ReviewRequest request){
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));

        validateRating(request.getRating());

        int rating = request.getRating();
        review.setRating(rating);
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);

        refreshBookRating(review.getBookId());

        return saved;
    }

    @CacheEvict(cacheNames = {
            CacheNames.REVIEWS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED
    }, allEntries = true)
    public void deleteReview(String id){
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));

        String bookId = review.getBookId();
        reviewRepository.delete(review);

        refreshBookRating(bookId);
    }

    private void validateRating(int rating){
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new RuntimeException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }
    }

    private void refreshBookRating(String bookId){
        bookRepository.findById(bookId).ifPresent(book -> {
            List<Review> reviews = reviewRepository.findByBookId(bookId);
            double average = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            double rounded = Math.round(average * 100.0) / 100.0;
            book.setAverageRating(rounded);
            book.setTotalReviews(reviews.size());
            bookRepository.save(book);
        });
    }

    private BookSummary buildBookSummary(Book book){
        return BookSummary.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .build();
    }

    private UserSummary buildUserSummary(User user){
        return UserSummary.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
