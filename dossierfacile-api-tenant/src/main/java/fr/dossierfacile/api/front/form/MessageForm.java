package fr.dossierfacile.api.front.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageForm {
    private Long tenantId;
    @NotBlank
    private String messageBody;
}
