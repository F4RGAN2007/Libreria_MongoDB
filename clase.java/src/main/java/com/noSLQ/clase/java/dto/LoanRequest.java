package com.noSLQ.clase.java.dto;


import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoanRequest {

    private String userId;
    private String bookId;
    private LocalDateTime expectedReturnDate;

}
