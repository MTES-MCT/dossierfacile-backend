package fr.gouv.bo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CallbackForm {
    @NotEmpty
    private String email;
    @NotEmpty
    private String source;
    private String internalPartnerId;
}
