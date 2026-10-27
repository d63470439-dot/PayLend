package com.paylend.model;

public class LoanAccount {
    public String loanId;
    public String userId;
    public double principalAmount;
    public double interestRate;
    public double totalPayable;
    public double remainingBalance;
    public String status;
    public String createdAt;

    public LoanAccount() {}
}
