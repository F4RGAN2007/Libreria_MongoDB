package com.noSLQ.clase.java.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Document(collection = "author")
public class Author {

    @Id
    private String id;
    private String name;
    private String nationality;
    private String biography;

}
