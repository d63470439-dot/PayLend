package com.paylend.model;

public class UserAccount {
    public String userId;
    public String name;
    public String mobileNumber;
    public String walletId;
    public double balance;

    public UserAccount() {}

    public UserAccount(String userId, String name, String mobileNumber, String walletId) {
        this.userId = userId;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.walletId = walletId;
        this.balance = 0.0;
    }
}
