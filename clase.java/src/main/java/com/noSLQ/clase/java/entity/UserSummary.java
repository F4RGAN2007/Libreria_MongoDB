package com.noSLQ.clase.java.entity;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserSummary {

    private String userId;
    private String name;
    private String email;

}
