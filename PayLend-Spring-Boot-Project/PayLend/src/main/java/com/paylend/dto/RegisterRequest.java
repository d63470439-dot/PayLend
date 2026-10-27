package com.paylend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Pattern(regexp = "[0-9+() -]{7,20}", message = "Enter a valid mobile number") String mobileNumber
) {}
