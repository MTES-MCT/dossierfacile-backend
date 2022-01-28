package fr.dossierfacile.api.dossierfacileapiowner.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NamesForm {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String email;
}
