package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.validator.annotation.tenant.honor_declaration.CheckHonorDeclarationClarification;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckHonorDeclarationClarification
public class HonorDeclarationForm implements FormWithTenantId {

    private Long tenantId;

    @AssertTrue
    private boolean honorDeclaration;

    @Size(max = 2000)
    private String clarification;
}
