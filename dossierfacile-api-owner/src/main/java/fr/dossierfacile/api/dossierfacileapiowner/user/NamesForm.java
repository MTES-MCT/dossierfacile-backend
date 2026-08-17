package fr.dossierfacile.api.dossierfacileapiowner.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NamesForm {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}
