package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.ReviewRequest;
import com.noSLQ.clase.java.entity.Review;
import com.noSLQ.clase.java.service.ReviewService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Review")
@AllArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{id}")
    public Review getReview(@PathVariable String id) {
        return reviewService.getById(id);
    }

    @PostMapping
    public Review saveReview(@RequestBody ReviewRequest review){
        return reviewService.createReview(review);
    }

    @GetMapping("all")
    public List<Review> getAllReviews(){
        return reviewService.getAll();
    }

    @GetMapping("/book/{bookId}")
    public List<Review> getReviewsByBook(@PathVariable String bookId){
        return reviewService.getByBook(bookId);
    }

    @GetMapping("/libro/{libroId}")
    public List<Review> getReviewsByLibro(@PathVariable String libroId){
        return reviewService.getByBook(libroId);
    }

    @GetMapping("/user/{userId}")
    public List<Review> getReviewsByUser(@PathVariable String userId){
        return reviewService.getByUser(userId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Review> getReviewsByUsuario(@PathVariable String usuarioId){
        return reviewService.getByUser(usuarioId);
    }

    @PatchMapping("/{id}")
    public Review updateReview(@PathVariable String id, @RequestBody ReviewRequest review){
        return reviewService.updateReview(id, review);
    }

    @DeleteMapping("/{id}")
    public String deleteReview(@PathVariable String id){
        reviewService.deleteReview(id);
        return "Review deleted with id: " + id;
    }
}
