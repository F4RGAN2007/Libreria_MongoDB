package com.noSLQ.clase.java.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoanStats {

    private long totalLoans;
    private long activeLoans;
    private long returnedLoans;
    private long overdueLoans;

}
