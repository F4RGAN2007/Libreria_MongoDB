package com.noSLQ.clase.java.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Document(collection = "review")
public class Review {

    @Id
    private String id;
    private String userId;
    private String bookId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    private BookSummary book;
    private UserSummary user;

}
