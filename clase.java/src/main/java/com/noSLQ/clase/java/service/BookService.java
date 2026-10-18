package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.BookRequest;
import com.noSLQ.clase.java.dto.BookResponse;
import com.noSLQ.clase.java.dto.PopularBook;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.repository.BookRepository;
import com.noSLQ.clase.java.repository.LoanRepository;
import com.noSLQ.clase.java.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@AllArgsConstructor
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final LoanRepository loanRepository;
    private final ConsistencyService consistencyService;

    @Cacheable(cacheNames = CacheNames.BOOKS)
    public List<Book> getAll(){
        return bookRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.BOOK_DETAIL, key = "#id")
    public Book getById(String id){
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    @Cacheable(cacheNames = CacheNames.BOOK_DETAIL, key = "'withReviews:' + #id")
    public BookResponse getByIdWithReviews(String id){
        Book book = getById(id);

        return BookResponse.builder()
                .book(book)
                .reviews(reviewRepository.findByBookIdOrderByCreatedAtDesc(id))
                .build();
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_BY_CATEGORY, key = "#category")
    public List<Book> getByCategory(String category){
        return bookRepository.findByCategory(category);
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_BY_AUTHOR, key = "#author")
    public List<Book> getByAuthor(String author){
        return bookRepository.findByAuthor(author);
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_AVAILABLE)
    public List<Book> getAvailableBooks(){
        return bookRepository.findByAvailable(true);
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_BORROWED)
    public List<Book> getBorrowedBooks(){
        return bookRepository.findByBorrowed(true);
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_BEST_RATED)
    public List<Book> getBestRated(){
        return bookRepository.findAllByOrderByAverageRatingDesc();
    }

    @Cacheable(cacheNames = CacheNames.BOOKS_POPULAR)
    public List<PopularBook> getPopular(){
        return loanRepository.findMostBorrowedBooks();
    }

    @CacheEvict(cacheNames = {
            CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED, CacheNames.BOOKS_POPULAR,
            CacheNames.REVIEWS, CacheNames.LOANS, CacheNames.FAVORITES
    }, allEntries = true)
    public Book createBook(BookRequest request){
        validateBook(request, null);

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(request.getCategory());
        book.setPublishYear(request.getPublishYear());
        book.setSynopsis(request.getSynopsis());
        if (request.getAvailable() != null) {
            book.setAvailable(request.getAvailable());
            book.setBorrowed(!request.getAvailable());
        } else {
            book.setAvailable(true);
            book.setBorrowed(false);
        }

        return bookRepository.save(book);
    }

    @CacheEvict(cacheNames = {
            CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED, CacheNames.BOOKS_POPULAR,
            CacheNames.REVIEWS, CacheNames.LOANS, CacheNames.FAVORITES
    }, allEntries = true)
    public Book updateBook(String id, BookRequest request){
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        validateBook(request, id);

        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategory(request.getCategory());
        book.setPublishYear(request.getPublishYear());
        book.setSynopsis(request.getSynopsis());
        if (request.getAvailable() != null) {
            book.setAvailable(request.getAvailable());
            book.setBorrowed(!request.getAvailable());
        }

        Book saved = bookRepository.save(book);

        consistencyService.syncBook(saved);

        return saved;
    }

    @CacheEvict(cacheNames = {
            CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_BEST_RATED, CacheNames.BOOKS_POPULAR,
            CacheNames.REVIEWS, CacheNames.LOANS, CacheNames.FAVORITES
    }, allEntries = true)
    public void deleteBook(String id){
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        if (book.isBorrowed()) {
            throw new RuntimeException("Cannot delete a borrowed book with id: " + id);
        }

        consistencyService.removeBookReferences(id);
        bookRepository.delete(book);
    }

    private void validateBook(BookRequest request, String currentBookId){
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Book title is required");
        }
        if (request.getIsbn() == null || request.getIsbn().isBlank()) {
            throw new RuntimeException("Book isbn is required");
        }

        bookRepository.findByIsbn(request.getIsbn())
                .filter(existing -> currentBookId == null || !currentBookId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new RuntimeException("Book already exists with isbn: " + request.getIsbn());
                });
    }

}
