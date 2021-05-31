package fr.dossierfacile.api.front.register.form.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HonorDeclarationForm {
    @AssertTrue
    private boolean honorDeclaration;
    private String clarification;
}
