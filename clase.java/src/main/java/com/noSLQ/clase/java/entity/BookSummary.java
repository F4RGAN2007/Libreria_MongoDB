package com.noSLQ.clase.java.entity;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BookSummary {

    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private String category;

}
