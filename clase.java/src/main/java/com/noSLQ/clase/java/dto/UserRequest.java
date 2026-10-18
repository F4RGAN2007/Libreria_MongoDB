package com.noSLQ.clase.java.dto;


import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserRequest {

    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime registrationDate;

}
