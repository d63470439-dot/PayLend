package com.paylend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotBlank String userId,
        @NotNull @DecimalMin(value = "1.0", message = "Loan amount must be at least 1") Double amount
) {}
