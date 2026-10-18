package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ReviewRequest {

    private String userId;
    private String bookId;
    private int rating;
    private String comment;

}
