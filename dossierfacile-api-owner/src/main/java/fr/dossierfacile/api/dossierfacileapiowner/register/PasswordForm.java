package fr.dossierfacile.api.dossierfacileapiowner.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordForm {
    @NotEmpty
    private String password;
}
