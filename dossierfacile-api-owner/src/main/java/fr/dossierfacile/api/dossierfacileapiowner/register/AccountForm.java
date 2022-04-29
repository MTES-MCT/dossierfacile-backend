package fr.dossierfacile.api.dossierfacileapiowner.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountForm {

    @Email
    @UniqueEmailActiveAccount
    private String email;

    @NotBlank
    private String password;

}
