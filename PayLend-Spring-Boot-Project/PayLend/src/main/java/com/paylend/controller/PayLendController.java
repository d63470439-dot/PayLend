package com.paylend.controller;

import com.paylend.dto.AmountRequest;
import com.paylend.dto.LoanRequest;
import com.paylend.dto.RegisterRequest;
import com.paylend.model.LoanAccount;
import com.paylend.model.TransactionRecord;
import com.paylend.model.UserAccount;
import com.paylend.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PayLendController {
    private final WalletService service;

    public PayLendController(WalletService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public List<UserAccount> users() { return service.users(); }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccount register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/wallet/add")
    public UserAccount addMoney(@Valid @RequestBody AmountRequest request) {
        return service.addMoney(request);
    }

    @PostMapping("/wallet/transfer")
    public Map<String, String> transfer(@Valid @RequestBody AmountRequest request) {
        service.transfer(request);
        return Map.of("message", "Transfer completed successfully.");
    }

    @PostMapping("/wallet/recharge")
    public Map<String, String> recharge(@Valid @RequestBody AmountRequest request) {
        service.recharge(request);
        return Map.of("message", "Recharge recorded successfully (demo only).");
    }

    @PostMapping("/loans")
    public LoanAccount requestLoan(@Valid @RequestBody LoanRequest request) {
        return service.requestLoan(request);
    }

    @PostMapping("/loans/repay")
    public LoanAccount repay(@Valid @RequestBody AmountRequest request) {
        return service.repay(request);
    }

    @GetMapping("/loans")
    public List<LoanAccount> loans(@RequestParam(required = false) String userId) {
        return service.loans(userId);
    }

    @GetMapping("/transactions")
    public List<TransactionRecord> transactions(@RequestParam(required = false) String userId) {
        return service.transactions(userId);
    }

    @GetMapping("/config")
    public Map<String, Double> config() {
        return Map.of("maxLoanAmount", service.loanLimit(), "flatInterestRate", service.interestRate());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage() == null ? "Invalid request." : ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("Please check your input.");
        return Map.of("error", message);
    }
}
