package com.noSLQ.clase.java.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Document(collection = "favorite")
public class Favorite {

    @Id
    private String id;
    private String userId;
    private String userName;

    @Builder.Default
    private List<FavoriteBook> books = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class FavoriteBook {

        private String bookId;
        private String title;
        private String author;
        private String isbn;
        private LocalDateTime addedAt;

    }

}
