package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.BookRequest;
import com.noSLQ.clase.java.dto.BookResponse;
import com.noSLQ.clase.java.dto.PopularBook;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.service.BookService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Book")
@AllArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable String id) {
        return bookService.getByIdWithReviews(id);
    }

    @PostMapping
    public Book saveBook(@RequestBody BookRequest book){
        return bookService.createBook(book);

    }

    @GetMapping("all")
    public List<Book> getAllBooks(){
        return bookService.getAll();
    }

    @PatchMapping("/{id}")
    public Book updateBook(@PathVariable String id, @RequestBody BookRequest book){
        return bookService.updateBook(id, book);
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable String id){
        bookService.deleteBook(id);
        return "Book deleted with id: " + id;
    }

    @GetMapping("/category/{category}")
    public List<Book> getBooksByCategory(@PathVariable String category){
        return bookService.getByCategory(category);
    }

    @GetMapping("/author/{author}")
    public List<Book> getBooksByAuthor(@PathVariable String author){
        return bookService.getByAuthor(author);
    }

    @GetMapping("/available")
    public List<Book> getAvailableBooks(){
        return bookService.getAvailableBooks();
    }

    @GetMapping("/disponibles")
    public List<Book> getDisponiblesBooks(){
        return bookService.getAvailableBooks();
    }

    @GetMapping("/borrowed")
    public List<Book> getBorrowedBooks(){
        return bookService.getBorrowedBooks();
    }

    @GetMapping("/prestados")
    public List<Book> getPrestadosBooks(){
        return bookService.getBorrowedBooks();
    }

    @GetMapping("/best-rated")
    public List<Book> getBestRatedBooks(){
        return bookService.getBestRated();
    }

    @GetMapping("/mejor-calificados")
    public List<Book> getMejorCalificados(){
        return bookService.getBestRated();
    }

    @GetMapping("/popular")
    public List<PopularBook> getPopularBooks(){
        return bookService.getPopular();
    }

    @GetMapping("/populares")
    public List<PopularBook> getPopularesBooks(){
        return bookService.getPopular();
    }
}
