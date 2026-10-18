package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.GenreRequest;
import com.noSLQ.clase.java.entity.Genre;
import com.noSLQ.clase.java.service.GenreService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Genre")
@AllArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping("/{id}")
    public Genre getGenre(@PathVariable String id) {
        return genreService.getById(id);
    }

    @PostMapping
    public Genre saveGenre(@RequestBody GenreRequest genre){
        return genreService.createGenre(genre);
    }

    @GetMapping("all")
    public List<Genre> getAllGenres(){
        return genreService.getAll();
    }

    @PatchMapping("/{id}")
    public Genre updateGenre(@PathVariable String id, @RequestBody GenreRequest genre){
        return genreService.updateGenre(id, genre);
    }

    @DeleteMapping("/{id}")
    public String deleteGenre(@PathVariable String id){
        genreService.deleteGenre(id);
        return "Genre deleted with id: " + id;
    }
}
