package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.BookSummary;
import com.noSLQ.clase.java.entity.Favorite;
import com.noSLQ.clase.java.entity.Loan;
import com.noSLQ.clase.java.entity.Review;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.entity.UserSummary;
import com.noSLQ.clase.java.repository.FavoriteRepository;
import com.noSLQ.clase.java.repository.LoanRepository;
import com.noSLQ.clase.java.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mantiene la consistencia de los datos denormalizados.
 *
 * El modelo guarda copias embebidas de libros y usuarios dentro de loans,
 * reviews y favorites (patron de denormalizacion estrategica para acelerar las
 * consultas frecuentes). Cuando el documento origen cambia, esas copias deben
 * actualizarse para no quedar desactualizadas.
 */
@AllArgsConstructor
@Service
public class ConsistencyService {

    private final LoanRepository loanRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;

    public void syncBook(Book book) {
        BookSummary summary = buildBookSummary(book);

        List<Loan> loans = loanRepository.findByBookId(book.getId());
        loans.forEach(loan -> loan.setBook(summary));
        loanRepository.saveAll(loans);

        List<Review> reviews = reviewRepository.findByBookId(book.getId());
        reviews.forEach(review -> review.setBook(summary));
        reviewRepository.saveAll(reviews);

        List<Favorite> favorites = favoriteRepository.findByBooksBookId(book.getId());
        for (Favorite favorite : favorites) {
            favorite.getBooks().stream()
                    .filter(favoriteBook -> book.getId().equals(favoriteBook.getBookId()))
                    .forEach(favoriteBook -> {
                        favoriteBook.setTitle(book.getTitle());
                        favoriteBook.setAuthor(book.getAuthor());
                        favoriteBook.setIsbn(book.getIsbn());
                    });
        }
        favoriteRepository.saveAll(favorites);
    }

    public void syncUser(User user) {
        UserSummary summary = buildUserSummary(user);

        List<Loan> loans = loanRepository.findByUserId(user.getId());
        loans.forEach(loan -> loan.setUser(summary));
        loanRepository.saveAll(loans);

        List<Review> reviews = reviewRepository.findByUserId(user.getId());
        reviews.forEach(review -> review.setUser(summary));
        reviewRepository.saveAll(reviews);

        favoriteRepository.findByUserId(user.getId()).ifPresent(favorite -> {
            favorite.setUserName(user.getName());
            favoriteRepository.save(favorite);
        });
    }

    public void removeBookReferences(String bookId) {
        loanRepository.deleteByBookId(bookId);
        reviewRepository.deleteByBookId(bookId);

        List<Favorite> favorites = favoriteRepository.findByBooksBookId(bookId);
        favorites.forEach(favorite ->
                favorite.getBooks().removeIf(favoriteBook -> bookId.equals(favoriteBook.getBookId())));
        favoriteRepository.saveAll(favorites);
    }

    public void removeUserReferences(String userId) {
        loanRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        favoriteRepository.findByUserId(userId).ifPresent(favoriteRepository::delete);
    }

    private BookSummary buildBookSummary(Book book) {
        return BookSummary.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .build();
    }

    private UserSummary buildUserSummary(User user) {
        return UserSummary.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

}
