package fr.dossierfacile.api.dossierfacileapiowner.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscribePropertyForm {

    @Email
    private String email;

    @NotBlank
    private Long tenantId;
}
