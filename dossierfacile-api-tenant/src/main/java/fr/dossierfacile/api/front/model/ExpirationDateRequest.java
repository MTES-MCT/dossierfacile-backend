package fr.dossierfacile.api.front.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpirationDateRequest {
    @NotNull(message = "Expiration date is required")
    @FutureOrPresent(message = "Expiration date must be in the present or future")
    private LocalDate expirationDate;
}
