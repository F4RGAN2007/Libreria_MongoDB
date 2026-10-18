package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.LoanRequest;
import com.noSLQ.clase.java.dto.LoanStats;
import com.noSLQ.clase.java.entity.Book;
import com.noSLQ.clase.java.entity.BookSummary;
import com.noSLQ.clase.java.entity.Loan;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.entity.UserSummary;
import com.noSLQ.clase.java.repository.BookRepository;
import com.noSLQ.clase.java.repository.LoanRepository;
import com.noSLQ.clase.java.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Service
public class LoanService {

    private static final int MAX_RECENT_LOANS = 5;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_RETURNED = "RETURNED";

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Cacheable(cacheNames = CacheNames.LOANS)
    public List<Loan> getAll(){
        return loanRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'id:' + #id")
    public Loan getById(String id){
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'user:' + #userId")
    public List<Loan> getByUser(String userId){
        return loanRepository.findByUserId(userId);
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'book:' + #bookId")
    public List<Loan> getByBook(String bookId){
        return loanRepository.findByBookId(bookId);
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'active'")
    public List<Loan> getActive(){
        return loanRepository.findByStatus(STATUS_ACTIVE);
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'user:' + #userId + ':status:' + #status")
    public List<Loan> getByUserAndStatus(String userId, String status){
        return loanRepository.findByUserIdAndStatus(userId, status);
    }

    @Cacheable(cacheNames = CacheNames.LOANS, key = "'stats'")
    public LoanStats getStats(){
        List<Loan> loans = loanRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long active = loans.stream()
                .filter(loan -> STATUS_ACTIVE.equals(loan.getStatus()))
                .count();
        long returned = loans.stream()
                .filter(loan -> STATUS_RETURNED.equals(loan.getStatus()))
                .count();
        long overdue = loans.stream()
                .filter(loan -> STATUS_ACTIVE.equals(loan.getStatus()))
                .filter(loan -> loan.getExpectedReturnDate() != null
                        && loan.getExpectedReturnDate().isBefore(now))
                .count();

        return LoanStats.builder()
                .totalLoans(loans.size())
                .activeLoans(active)
                .returnedLoans(returned)
                .overdueLoans(overdue)
                .build();
    }

    @CacheEvict(cacheNames = {
            CacheNames.LOANS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_POPULAR, CacheNames.USERS
    }, allEntries = true)
    public Loan createLoan(LoanRequest request){
        if (request.getExpectedReturnDate() == null
                || !request.getExpectedReturnDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Expected return date must be in the future");
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + request.getBookId()));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        if (book.isBorrowed()) {
            throw new RuntimeException("Book is already borrowed with id: " + book.getId());
        }

        LocalDateTime loanDate = LocalDateTime.now();

        Loan loan = new Loan();
        loan.setUserId(user.getId());
        loan.setBookId(book.getId());
        loan.setLoanDate(loanDate);
        loan.setExpectedReturnDate(request.getExpectedReturnDate());
        loan.setStatus(STATUS_ACTIVE);
        loan.setBook(buildBookSummary(book));
        loan.setUser(buildUserSummary(user));

        Loan saved = loanRepository.save(loan);

        book.setBorrowed(true);
        book.setAvailable(false);
        bookRepository.save(book);

        addRecentLoan(user, saved);

        return saved;
    }

    @CacheEvict(cacheNames = {
            CacheNames.LOANS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_POPULAR, CacheNames.USERS
    }, allEntries = true)
    public Loan returnLoan(String id){
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));

        if (STATUS_RETURNED.equals(loan.getStatus())) {
            throw new RuntimeException("Loan already returned with id: " + id);
        }

        loan.setActualReturnDate(LocalDateTime.now());
        loan.setStatus(STATUS_RETURNED);
        Loan saved = loanRepository.save(loan);

        bookRepository.findById(loan.getBookId()).ifPresent(book -> {
            book.setBorrowed(false);
            book.setAvailable(true);
            bookRepository.save(book);
        });

        userRepository.findById(loan.getUserId()).ifPresent(user -> {
            if (user.getRecentActivity() != null && user.getRecentActivity().getLastLoans() != null) {
                user.getRecentActivity().getLastLoans().stream()
                        .filter(summary -> id.equals(summary.getLoanId()))
                        .forEach(summary -> summary.setStatus(STATUS_RETURNED));
                userRepository.save(user);
            }
        });

        return saved;
    }

    @CacheEvict(cacheNames = {
            CacheNames.LOANS, CacheNames.BOOKS, CacheNames.BOOK_DETAIL, CacheNames.BOOKS_BY_CATEGORY,
            CacheNames.BOOKS_BY_AUTHOR, CacheNames.BOOKS_AVAILABLE, CacheNames.BOOKS_BORROWED,
            CacheNames.BOOKS_POPULAR, CacheNames.USERS
    }, allEntries = true)
    public void deleteLoan(String id){
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));

        if (STATUS_ACTIVE.equals(loan.getStatus())) {
            bookRepository.findById(loan.getBookId()).ifPresent(book -> {
                book.setBorrowed(false);
                book.setAvailable(true);
                bookRepository.save(book);
            });
        }

        userRepository.findById(loan.getUserId()).ifPresent(user -> {
            if (user.getRecentActivity() != null && user.getRecentActivity().getLastLoans() != null) {
                user.getRecentActivity().getLastLoans()
                        .removeIf(summary -> id.equals(summary.getLoanId()));
                userRepository.save(user);
            }
        });

        loanRepository.delete(loan);
    }

    private BookSummary buildBookSummary(Book book){
        return BookSummary.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .build();
    }

    private UserSummary buildUserSummary(User user){
        return UserSummary.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    private void addRecentLoan(User user, Loan loan){
        if (user.getRecentActivity() == null) {
            user.setRecentActivity(new User.RecentActivity());
        }

        List<User.LoanSummary> lastLoans = new ArrayList<>(user.getRecentActivity().getLastLoans());
        lastLoans.add(0, User.LoanSummary.builder()
                .loanId(loan.getId())
                .bookId(loan.getBookId())
                .title(loan.getBook() != null ? loan.getBook().getTitle() : null)
                .author(loan.getBook() != null ? loan.getBook().getAuthor() : null)
                .loanDate(loan.getLoanDate())
                .expectedReturnDate(loan.getExpectedReturnDate())
                .status(loan.getStatus())
                .build());

        if (lastLoans.size() > MAX_RECENT_LOANS) {
            lastLoans = lastLoans.subList(0, MAX_RECENT_LOANS);
        }

        user.getRecentActivity().setLastLoans(lastLoans);
        user.getRecentActivity().setLastLoanDate(loan.getLoanDate());
        userRepository.save(user);
    }

}
