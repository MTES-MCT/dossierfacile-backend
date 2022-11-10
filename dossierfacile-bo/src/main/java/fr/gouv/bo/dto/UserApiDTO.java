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

    @NotBlank
    private String urlCallback;

    @NotBlank
    private String name;

    private String name2;
    private String site;
    @NotNull
    private TypeUserApi typeUserApi;
    private String textModal;
    private String partnerApiKeyCallback;
    @NotNull
    private Integer version;

}
