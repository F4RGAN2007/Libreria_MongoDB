package com.noSLQ.clase.java.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Document(collection = "book")
public class Book {

    @Id
    private String id;
    private String title;
    private String author;
    private String isbn;
    private String synopsis;
    private String publishYear;
    private String category;
    @Builder.Default
    private boolean available = true;
    @Builder.Default
    private boolean borrowed = false;
    @Builder.Default
    private double averageRating = 0.0;
    @Builder.Default
    private int totalReviews = 0;

}
