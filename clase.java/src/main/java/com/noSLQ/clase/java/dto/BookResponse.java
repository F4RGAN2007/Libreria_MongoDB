package com.noSLQ.clase.java.dto;


import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.Review;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BookResponse {

    private Book book;

    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

}
