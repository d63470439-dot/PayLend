package com.paylend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AmountRequest(
        @NotBlank String userId,
        @NotNull @DecimalMin(value = "1.0", message = "Amount must be at least 1") Double amount,
        @Size(max = 30) String mobileNumber,
        @Size(max = 60) String operator,
        String receiverId
) {}
