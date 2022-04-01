package fr.gouv.bo.dto;

import fr.gouv.bo.validator.annotation.ExistEmail;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;

@Getter
@Setter
public class DeleteUserDTO {

    @Email
    @ExistEmail(message = "Il n'y a pas de compte associé à cette adresse email")
    private String email;

}
