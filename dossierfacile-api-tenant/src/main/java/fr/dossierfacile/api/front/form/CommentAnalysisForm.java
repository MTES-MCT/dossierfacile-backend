package fr.dossierfacile.api.front.form;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentAnalysisForm implements FormWithTenantId {
    private Long documentId;
    private Long tenantId;
    @NotBlank
    private String comment;
}
