package fr.dossierfacile.api.front.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerForm {
    @NotEmpty
    private String source;

    private String internalPartnerId;
}
