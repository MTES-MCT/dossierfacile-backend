package fr.gouv.owner.dto;

import fr.gouv.owner.annotation.UniqueEmail;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.owner.validator.interfaces.CreateUser;
import fr.gouv.owner.validator.interfaces.UpdateUser;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class UserDTO {

    private Long id;

    @NotBlank(groups = {CreateUser.class, UpdateUser.class})
    @Size(min = 1, max = 50, groups = {CreateUser.class, UpdateUser.class})
    private String firstName;

    @NotBlank(groups = {CreateUser.class, UpdateUser.class})
    @Size(min = 1, max = 50, groups = {CreateUser.class, UpdateUser.class})
    private String lastName;

    @Email(groups = CreateUser.class)
    @UniqueEmail(message = "Cette adresse email est déjà utilisée", groups = CreateUser.class)
    private String email;

    @NotNull(groups = {CreateUser.class, UpdateUser.class})
    private List<Role> role;
}
