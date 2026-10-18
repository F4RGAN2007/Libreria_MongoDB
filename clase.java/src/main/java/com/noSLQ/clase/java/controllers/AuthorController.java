package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.AuthorRequest;
import com.noSLQ.clase.java.entity.Author;
import com.noSLQ.clase.java.service.AuthorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Author")
@AllArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping("/{id}")
    public Author getAuthor(@PathVariable String id) {
        return authorService.getById(id);
    }

    @PostMapping
    public Author saveAuthor(@RequestBody AuthorRequest author){
        return authorService.createAuthor(author);
    }

    @GetMapping("all")
    public List<Author> getAllAuthors(){
        return authorService.getAll();
    }

    @PatchMapping("/{id}")
    public Author updateAuthor(@PathVariable String id, @RequestBody AuthorRequest author){
        return authorService.updateAuthor(id, author);
    }

    @DeleteMapping("/{id}")
    public String deleteAuthor(@PathVariable String id){
        authorService.deleteAuthor(id);
        return "Author deleted with id: " + id;
    }
}
