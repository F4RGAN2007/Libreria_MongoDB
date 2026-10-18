package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.FavoriteRequest;
import com.noSLQ.clase.java.entity.Favorite;
import com.noSLQ.clase.java.service.FavoriteService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Favorite")
@AllArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("all")
    public List<Favorite> getAllFavorites(){
        return favoriteService.getAll();
    }

    @GetMapping("/user/{userId}")
    public Favorite getFavoritesByUser(@PathVariable String userId){
        return favoriteService.getByUser(userId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public Favorite getFavoritesByUsuario(@PathVariable String usuarioId){
        return favoriteService.getByUser(usuarioId);
    }

    @PostMapping("/user/{userId}")
    public Favorite addFavoriteBook(@PathVariable String userId, @RequestBody FavoriteRequest favorite){
        return favoriteService.addBook(userId, favorite);
    }

    @PostMapping("/usuario/{usuarioId}")
    public Favorite addFavoriteBookUsuario(@PathVariable String usuarioId, @RequestBody FavoriteRequest favorite){
        return favoriteService.addBook(usuarioId, favorite);
    }

    @DeleteMapping("/user/{userId}/book/{bookId}")
    public Favorite removeFavoriteBook(@PathVariable String userId, @PathVariable String bookId){
        return favoriteService.removeBook(userId, bookId);
    }

    @DeleteMapping("/usuario/{usuarioId}/libro/{libroId}")
    public Favorite removeFavoriteBookUsuario(@PathVariable String usuarioId, @PathVariable String libroId){
        return favoriteService.removeBook(usuarioId, libroId);
    }

    @DeleteMapping("/user/{userId}")
    public String deleteFavorites(@PathVariable String userId){
        favoriteService.deleteFavorite(userId);
        return "Favorites deleted for user: " + userId;
    }

    @DeleteMapping("/usuario/{usuarioId}")
    public String deleteFavoritesUsuario(@PathVariable String usuarioId){
        favoriteService.deleteFavorite(usuarioId);
        return "Favorites deleted for user: " + usuarioId;
    }
}
