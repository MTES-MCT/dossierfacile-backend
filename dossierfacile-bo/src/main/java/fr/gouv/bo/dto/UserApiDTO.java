package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.TypeUserApi;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
    @NotNull
    private TypeUserApi typeUserApi;
    private String partnerApiKeyCallback;
    @NotNull
    private Integer version;
    private boolean disabled;
    private String logoUrl;
    private String welcomeUrl;
    private String completedUrl;
    private String deniedUrl;
    private String validatedUrl;
}
