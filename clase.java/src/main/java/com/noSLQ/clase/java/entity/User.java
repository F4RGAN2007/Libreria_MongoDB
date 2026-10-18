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
@Document(collection = "user")
public class User {

    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime registrationDate;

    @Builder.Default
    private RecentActivity recentActivity = new RecentActivity();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class RecentActivity {

        @Builder.Default
        private List<LoanSummary> lastLoans = new ArrayList<>();
        private LocalDateTime lastLoanDate;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class LoanSummary {

        private String loanId;
        private String bookId;
        private String title;
        private String author;
        private LocalDateTime loanDate;
        private LocalDateTime expectedReturnDate;
        private String status;

    }

}
