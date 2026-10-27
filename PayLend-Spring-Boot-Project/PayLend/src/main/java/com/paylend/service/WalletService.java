package com.paylend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylend.dto.AmountRequest;
import com.paylend.dto.LoanRequest;
import com.paylend.dto.RegisterRequest;
import com.paylend.model.LoanAccount;
import com.paylend.model.PayLendData;
import com.paylend.model.TransactionRecord;
import com.paylend.model.UserAccount;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {
    // Demo configuration. Change these to match your original console project if needed.
    private static final double MAX_LOAN_AMOUNT = 10000.0;
    private static final double FLAT_INTEREST_RATE = 10.0; // percent of principal

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Path dataFile = Path.of("data", "paylend-data.json");
    private PayLendData data;

    public WalletService() {
        try {
            Files.createDirectories(dataFile.getParent());
            if (Files.exists(dataFile) && Files.size(dataFile) > 0) {
                data = mapper.readValue(dataFile.toFile(), PayLendData.class);
                if (data.users == null) data.users = new java.util.LinkedHashMap<>();
                if (data.loans == null) data.loans = new java.util.LinkedHashMap<>();
                if (data.transactions == null) data.transactions = new ArrayList<>();
            } else {
                data = new PayLendData();
                save();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load PayLend data file", e);
        }
    }

    public synchronized List<UserAccount> users() {
        return data.users.values().stream()
                .sorted(Comparator.comparing(u -> u.name.toLowerCase()))
                .toList();
    }

    public synchronized UserAccount register(RegisterRequest request) {
        String mobile = request.mobileNumber().trim();
        boolean duplicate = data.users.values().stream().anyMatch(u -> u.mobileNumber.equals(mobile));
        if (duplicate) throw new IllegalArgumentException("A user with this mobile number already exists.");
        String id = "USR-" + shortId();
        UserAccount user = new UserAccount(id, request.name().trim(), mobile, "WAL-" + shortId());
        data.users.put(id, user);
        addTransaction("REGISTRATION", id, null, 0, "Wallet created for " + user.name, "SUCCESS");
        save();
        return user;
    }

    public synchronized UserAccount addMoney(AmountRequest request) {
        UserAccount user = user(request.userId());
        double amount = validAmount(request.amount());
        user.balance = money(user.balance + amount);
        addTransaction("ADD_MONEY", user.userId, null, amount, "Money added to wallet", "SUCCESS");
        save();
        return user;
    }

    public synchronized void transfer(AmountRequest request) {
        UserAccount sender = user(request.userId());
        UserAccount receiver = user(request.receiverId() == null ? "" : request.receiverId());
        double amount = validAmount(request.amount());
        if (sender.userId.equals(receiver.userId)) throw new IllegalArgumentException("You cannot transfer money to yourself.");
        if (sender.balance < amount) throw new IllegalArgumentException("Insufficient wallet balance.");
        sender.balance = money(sender.balance - amount);
        receiver.balance = money(receiver.balance + amount);
        addTransaction("TRANSFER_SENT", sender.userId, receiver.name, amount, "Transfer to " + receiver.name, "SUCCESS");
        addTransaction("TRANSFER_RECEIVED", receiver.userId, sender.name, amount, "Transfer from " + sender.name, "SUCCESS");
        save();
    }

    public synchronized void recharge(AmountRequest request) {
        UserAccount user = user(request.userId());
        double amount = validAmount(request.amount());
        String mobile = request.mobileNumber() == null ? "" : request.mobileNumber().trim();
        if (!mobile.matches("[0-9+() -]{7,20}")) throw new IllegalArgumentException("Enter a valid mobile number.");
        if (request.operator() == null || request.operator().isBlank()) throw new IllegalArgumentException("Choose a mobile operator.");
        if (user.balance < amount) throw new IllegalArgumentException("Insufficient wallet balance.");
        user.balance = money(user.balance - amount);
        addTransaction("MOBILE_RECHARGE", user.userId, mobile, amount,
                "Simulated recharge for " + mobile + " (" + request.operator().trim() + ")", "SUCCESS");
        save();
    }

    public synchronized LoanAccount requestLoan(LoanRequest request) {
        UserAccount user = user(request.userId());
        double amount = validAmount(request.amount());
        if (amount > MAX_LOAN_AMOUNT) throw new IllegalArgumentException("The maximum demo loan amount is ₹" + (int) MAX_LOAN_AMOUNT + ".");
        boolean active = data.loans.values().stream()
                .anyMatch(l -> l.userId.equals(user.userId) && l.remainingBalance > 0.001);
        if (active) throw new IllegalArgumentException("Repay your active loan before requesting another.");
        LoanAccount loan = new LoanAccount();
        loan.loanId = "LOAN-" + shortId();
        loan.userId = user.userId;
        loan.principalAmount = amount;
        loan.interestRate = FLAT_INTEREST_RATE;
        loan.totalPayable = money(amount * (1 + FLAT_INTEREST_RATE / 100.0));
        loan.remainingBalance = loan.totalPayable;
        loan.status = "ACTIVE";
        loan.createdAt = now();
        data.loans.put(loan.loanId, loan);
        user.balance = money(user.balance + amount);
        addTransaction("LOAN_DISBURSEMENT", user.userId, loan.loanId, amount,
                "Micro-loan approved (demo) with " + FLAT_INTEREST_RATE + "% flat interest", "SUCCESS");
        save();
        return loan;
    }

    public synchronized LoanAccount repay(AmountRequest request) {
        UserAccount user = user(request.userId());
        double amount = validAmount(request.amount());
        LoanAccount loan = data.loans.values().stream()
                .filter(l -> l.userId.equals(user.userId) && l.remainingBalance > 0.001)
                .min(Comparator.comparing(l -> l.createdAt))
                .orElseThrow(() -> new IllegalArgumentException("No active loan to repay."));
        if (amount > loan.remainingBalance) amount = loan.remainingBalance;
        if (user.balance < amount) throw new IllegalArgumentException("Insufficient wallet balance for this repayment.");
        user.balance = money(user.balance - amount);
        loan.remainingBalance = money(loan.remainingBalance - amount);
        if (loan.remainingBalance <= 0.001) {
            loan.remainingBalance = 0;
            loan.status = "PAID";
        }
        addTransaction("LOAN_REPAYMENT", user.userId, loan.loanId, amount, "Loan repayment instalment", "SUCCESS");
        save();
        return loan;
    }

    public synchronized List<LoanAccount> loans(String userId) {
        if (userId == null || userId.isBlank()) return data.loans.values().stream().toList();
        user(userId);
        return data.loans.values().stream().filter(l -> l.userId.equals(userId)).toList();
    }

    public synchronized List<TransactionRecord> transactions(String userId) {
        if (userId == null || userId.isBlank()) return data.transactions.stream()
                .sorted(Comparator.comparing((TransactionRecord t) -> t.dateTime).reversed()).toList();
        user(userId);
        return data.transactions.stream().filter(t -> t.userId.equals(userId))
                .sorted(Comparator.comparing((TransactionRecord t) -> t.dateTime).reversed()).toList();
    }

    public synchronized double loanLimit() { return MAX_LOAN_AMOUNT; }
    public synchronized double interestRate() { return FLAT_INTEREST_RATE; }

    private UserAccount user(String id) {
        UserAccount user = data.users.get(id);
        if (user == null) throw new IllegalArgumentException("User not found. Register or select a valid user first.");
        return user;
    }

    private double validAmount(Double value) {
        if (value == null || !Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        return money(value);
    }

    private double money(double value) { return Math.round(value * 100.0) / 100.0; }
    private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(); }
    private String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }

    private void addTransaction(String type, String userId, String otherParty, double amount, String description, String status) {
        TransactionRecord t = new TransactionRecord();
        t.transactionId = "TXN-" + shortId();
        t.type = type;
        t.userId = userId;
        t.otherParty = otherParty;
        t.amount = money(amount);
        t.dateTime = now();
        t.status = status;
        t.description = description;
        data.transactions.add(t);
    }

    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), data);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save PayLend data", e);
        }
    }
}
