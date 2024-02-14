package fr.gouv.bo.dto;

import fr.gouv.bo.validator.annotation.ExistEmail;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteUserDTO {

    @Email
    @ExistEmail(message = "Il n'y a pas de compte associé à cette adresse email")
    private String email;

}
