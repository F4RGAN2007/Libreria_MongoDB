package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PopularBook {

    private String bookId;
    private String title;
    private String author;
    private long totalLoans;

}
