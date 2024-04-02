package fr.dossierfacile.api.front.form;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageForm implements FormWithTenantId {
    private Long tenantId;
    @NotBlank
    private String messageBody;
}
