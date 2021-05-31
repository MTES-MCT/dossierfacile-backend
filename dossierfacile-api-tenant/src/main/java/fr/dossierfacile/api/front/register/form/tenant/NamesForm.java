package fr.dossierfacile.api.front.register.form.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NamesForm {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String zipCode;
}
