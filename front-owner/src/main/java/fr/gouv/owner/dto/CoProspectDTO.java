package fr.gouv.owner.dto;

import fr.gouv.owner.annotation.UniqueEmail;
import fr.gouv.owner.validator.interfaces.CreateProspect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoProspectDTO {

    private String firstName;
    private String lastName;
    @Email(groups = {CreateProspect.class})
    @UniqueEmail(message = "Cette adresse email est déjà utilisée", groups = {CreateProspect.class})
    private String email;
}
