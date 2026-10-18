package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.GenreRequest;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.Genre;
import com.noSLQ.clase.java.repository.BookRepository;
import com.noSLQ.clase.java.repository.GenreRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@AllArgsConstructor
@Service
public class GenreService {

    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;
    private final ConsistencyService consistencyService;

    @Cacheable(cacheNames = CacheNames.GENRES)
    public List<Genre> getAll(){
        return genreRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.GENRES, key = "#id")
    public Genre getById(String id){
        return genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with id: " + id));
    }

    @CacheEvict(cacheNames = CacheNames.GENRES, allEntries = true)
    public Genre createGenre(GenreRequest request){
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Genre name is required");
        }

        Genre genre = new Genre();
        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        return genreRepository.save(genre);
    }

    @CacheEvict(cacheNames = {
            CacheNames.GENRES, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED, CacheNames.BOOKS_POPULAR,
            CacheNames.REVIEWS, CacheNames.LOANS, CacheNames.FAVORITES
    }, allEntries = true)
    public Genre updateGenre(String id, GenreRequest request){
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with id: " + id));

        String previousName = genre.getName();

        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        Genre saved = genreRepository.save(genre);

        if (previousName != null && !previousName.equals(saved.getName())) {
            List<Book> books = bookRepository.findByCategory(previousName);
            for (Book book : books) {
                book.setCategory(saved.getName());
                consistencyService.syncBook(bookRepository.save(book));
            }
        }

        return saved;
    }

    @CacheEvict(cacheNames = CacheNames.GENRES, allEntries = true)
    public void deleteGenre(String id){
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Genre not found with id: " + id));

        genreRepository.delete(genre);
    }

}
