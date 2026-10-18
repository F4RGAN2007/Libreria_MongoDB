package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.AuthorRequest;
import com.noSLQ.clase.java.entity.Author;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.repository.AuthorRepository;
import com.noSLQ.clase.java.repository.BookRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@AllArgsConstructor
@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final ConsistencyService consistencyService;

    @Cacheable(cacheNames = CacheNames.AUTHORS)
    public List<Author> getAll(){
        return authorRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.AUTHORS, key = "#id")
    public Author getById(String id){
        return authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));
    }

    @CacheEvict(cacheNames = CacheNames.AUTHORS, allEntries = true)
    public Author createAuthor(AuthorRequest request){
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Author name is required");
        }

        Author author = new Author();
        author.setName(request.getName());
        author.setNationality(request.getNationality());
        author.setBiography(request.getBiography());

        return authorRepository.save(author);
    }

    @CacheEvict(cacheNames = {
            CacheNames.AUTHORS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED, CacheNames.BOOKS_POPULAR,
            CacheNames.REVIEWS, CacheNames.LOANS, CacheNames.FAVORITES
    }, allEntries = true)
    public Author updateAuthor(String id, AuthorRequest request){
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));

        String previousName = author.getName();

        author.setName(request.getName());
        author.setNationality(request.getNationality());
        author.setBiography(request.getBiography());

        Author saved = authorRepository.save(author);

        if (previousName != null && !previousName.equals(saved.getName())) {
            List<Book> books = bookRepository.findByAuthor(previousName);
            for (Book book : books) {
                book.setAuthor(saved.getName());
                consistencyService.syncBook(bookRepository.save(book));
            }
        }

        return saved;
    }

    @CacheEvict(cacheNames = CacheNames.AUTHORS, allEntries = true)
    public void deleteAuthor(String id){
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));

        authorRepository.delete(author);
    }

}
