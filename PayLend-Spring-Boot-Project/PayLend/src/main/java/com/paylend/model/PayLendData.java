package com.paylend.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PayLendData {
    public Map<String, UserAccount> users = new LinkedHashMap<>();
    public Map<String, LoanAccount> loans = new LinkedHashMap<>();
    public List<TransactionRecord> transactions = new ArrayList<>();
}
