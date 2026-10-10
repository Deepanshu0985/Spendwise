package com.finance.infrastructure.web.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The conversation so far, oldest first, ending with the user's new question. The server stores none of it. */
public record ChatRequest(@NotEmpty @Size(max = 10) List<@Valid @NotNull Turn> messages) {

    public record Turn(@NotNull String role, @NotNull @Size(max = 1000) String content) {
    }
}
