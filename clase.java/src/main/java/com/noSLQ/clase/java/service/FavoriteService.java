package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.FavoriteRequest;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.Favorite;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.repository.BookRepository;
import com.noSLQ.clase.java.repository.FavoriteRepository;
import com.noSLQ.clase.java.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Cacheable(cacheNames = CacheNames.FAVORITES)
    public List<Favorite> getAll(){
        return favoriteRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.FAVORITES, key = "'user:' + #userId")
    public Favorite getByUser(String userId){
        return favoriteRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Favorite list not found for user: " + userId));
    }

    @CacheEvict(cacheNames = CacheNames.FAVORITES, allEntries = true)
    public Favorite addBook(String userId, FavoriteRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + request.getBookId()));

        Favorite favorite = favoriteRepository.findByUserId(userId)
                .orElseGet(() -> Favorite.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .books(new ArrayList<>())
                        .build());

        boolean alreadyFavorite = favorite.getBooks().stream()
                .anyMatch(favoriteBook -> book.getId().equals(favoriteBook.getBookId()));

        if (alreadyFavorite) {
            throw new RuntimeException("Book already in favorites with id: " + book.getId());
        }

        Favorite.FavoriteBook fb = Favorite.FavoriteBook.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .addedAt(LocalDateTime.now())
                .build();
        favorite.getBooks().add(fb);

        return favoriteRepository.save(favorite);
    }

    @CacheEvict(cacheNames = CacheNames.FAVORITES, allEntries = true)
    public Favorite removeBook(String userId, String bookId){
        Favorite favorite = favoriteRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Favorite list not found for user: " + userId));

        favorite.getBooks().removeIf(favoriteBook -> bookId.equals(favoriteBook.getBookId()));

        return favoriteRepository.save(favorite);
    }

    @CacheEvict(cacheNames = CacheNames.FAVORITES, allEntries = true)
    public void deleteFavorite(String userId){
        Favorite favorite = favoriteRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Favorite list not found for user: " + userId));

        favoriteRepository.delete(favorite);
    }

}
