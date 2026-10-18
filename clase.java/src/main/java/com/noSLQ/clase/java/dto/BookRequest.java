package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BookRequest {

    private String title;
    private String author;
    private String isbn;
    private String synopsis;
    private String publishYear;
    private String category;
    private Boolean available;

}
