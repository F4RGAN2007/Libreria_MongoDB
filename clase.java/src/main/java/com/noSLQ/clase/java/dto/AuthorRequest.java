package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AuthorRequest {

    private String name;
    private String nationality;
    private String biography;

}
