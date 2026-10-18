package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.LoanRequest;
import com.noSLQ.clase.java.dto.LoanStats;
import com.noSLQ.clase.java.entity.Loan;
import com.noSLQ.clase.java.service.LoanService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Loan")
@AllArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/{id}")
    public Loan getLoan(@PathVariable String id) {
        return loanService.getById(id);
    }

    @PostMapping
    public Loan saveLoan(@RequestBody LoanRequest loan){
        return loanService.createLoan(loan);
    }

    @GetMapping("all")
    public List<Loan> getAllLoans(){
        return loanService.getAll();
    }

    @GetMapping("/active")
    public List<Loan> getActiveLoans(){
        return loanService.getActive();
    }

    @GetMapping("/activos")
    public List<Loan> getActivosLoans(){
        return loanService.getActive();
    }

    @GetMapping("/stats")
    public LoanStats getLoanStats(){
        return loanService.getStats();
    }

    @GetMapping("/estadisticas")
    public LoanStats getEstadisticas(){
        return loanService.getStats();
    }

    @GetMapping("/user/{userId}")
    public List<Loan> getLoansByUser(@PathVariable String userId){
        return loanService.getByUser(userId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Loan> getLoansByUsuario(@PathVariable String usuarioId){
        return loanService.getByUser(usuarioId);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public List<Loan> getLoansByUserAndStatus(@PathVariable String userId, @PathVariable String status){
        return loanService.getByUserAndStatus(userId, status);
    }

    @GetMapping("/usuario/{usuarioId}/estado/{estado}")
    public List<Loan> getLoansByUsuarioAndEstado(@PathVariable String usuarioId, @PathVariable String estado){
        return loanService.getByUserAndStatus(usuarioId, estado);
    }

    @GetMapping("/book/{bookId}")
    public List<Loan> getLoansByBook(@PathVariable String bookId){
        return loanService.getByBook(bookId);
    }

    @GetMapping("/libro/{libroId}")
    public List<Loan> getLoansByLibro(@PathVariable String libroId){
        return loanService.getByBook(libroId);
    }

    @PatchMapping("/{id}/return")
    public Loan returnLoan(@PathVariable String id){
        return loanService.returnLoan(id);
    }

    @PatchMapping("/{id}/devolver")
    public Loan devolverLoan(@PathVariable String id){
        return loanService.returnLoan(id);
    }

    @DeleteMapping("/{id}")
    public String deleteLoan(@PathVariable String id){
        loanService.deleteLoan(id);
        return "Loan deleted with id: " + id;
    }
}
