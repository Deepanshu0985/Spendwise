package com.finance.infrastructure.web.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** currency is required on create and ignored on update; currentAmount defaults to zero. */
public record SaveGoalRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal targetAmount,
        @DecimalMin(value = "0") @Digits(integer = 15, fraction = 2) BigDecimal currentAmount,
        LocalDate targetDate,
        @Size(min = 3, max = 3) String currency) {
}
