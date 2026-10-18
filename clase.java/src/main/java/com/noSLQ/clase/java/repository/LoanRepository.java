package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.dto.PopularBook;
import com.noSLQ.clase.java.entity.Loan;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {

    List<Loan> findByUserId(String userId);
    List<Loan> findByBookId(String bookId);
    List<Loan> findByStatus(String status);
    List<Loan> findByUserIdAndStatus(String userId, String status);
    List<Loan> findByUserIdOrderByLoanDateDesc(String userId);
    long countByBookId(String bookId);
    long countByUserId(String userId);
    long countByStatus(String status);

    void deleteByBookId(String bookId);
    void deleteByUserId(String userId);

    @Aggregation(pipeline = {
            "{ $group: { _id: '$bookId', totalLoans: { $sum: 1 }, title: { $first: '$book.title' }, author: { $first: '$book.author' } } }",
            "{ $project: { _id: 0, bookId: '$_id', title: 1, author: 1, totalLoans: 1 } }",
            "{ $sort: { totalLoans: -1 } }",
            "{ $limit: 10 }"
    })
    List<PopularBook> findMostBorrowedBooks();

}
