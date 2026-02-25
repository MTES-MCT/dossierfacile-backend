package fr.gouv.bo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserApiDTO {
    private Long id;
    private String urlCallback;
    @NotBlank
    private String name;
    private String name2;
    private String site;
    @NotBlank
    @Email
    private String email;
    private String partnerApiKeyCallback;
    private boolean disabled;
    private String logoUrl;
}
