package fr.gouv.owner.dto;

import fr.gouv.owner.annotation.UniqueEmail;
import fr.dossierfacile.common.entity.Owner;
import fr.gouv.owner.validator.interfaces.Step1RegisterOwner;
import fr.gouv.owner.validator.interfaces.Step2RegisterOwner;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OwnerDTO {

    private Long id;
    @NotBlank(groups = Step1RegisterOwner.class)
    @Size(min = 1, max = 50, groups = Step1RegisterOwner.class)
    private String firstName;
    @NotBlank(groups = Step1RegisterOwner.class)
    @Size(min = 1, max = 50, groups = Step1RegisterOwner.class)
    private String lastName;
    @Email(groups = Step2RegisterOwner.class)
    @UniqueEmail(message = "Cette adresse email est déjà utilisée", groups = Step2RegisterOwner.class)
    private String email;
    @NotBlank(groups = {Step2RegisterOwner.class})
    private String password;
    private String slug;

    private String address;
    private Double cost;

    public OwnerDTO(Owner owner) {
        this.id = owner.getId();
        this.slug = owner.getSlug();
    }
}
