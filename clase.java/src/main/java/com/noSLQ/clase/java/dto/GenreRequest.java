package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class GenreRequest {

    private String name;
    private String description;

}
