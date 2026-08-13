package fr.dossierfacile.api.front.form;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequestForm {

    // true = the tenant wants an operator validation, false = the tenant declines it
    @NotNull
    private Boolean validationRequested;
}
