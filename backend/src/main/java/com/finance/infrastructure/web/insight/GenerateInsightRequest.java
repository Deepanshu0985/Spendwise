package com.finance.infrastructure.web.insight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** month is yyyy-mm. refresh asks for a new one even if the figures have not changed. */
public record GenerateInsightRequest(@NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month, Boolean refresh) {
}
