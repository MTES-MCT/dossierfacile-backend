package fr.gouv.owner.dto;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserApiDTO {

    @NotBlank
    private String urlCallback;

    @NotBlank
    private String name;

    private String name2;
    private String site;
    private Long id;
    private TypeUserApi typeUserApi;
    private MultipartFile logo;
    private String textModal;
    private String partnerApiKeyCallback;

    public UserApiDTO(String name, TypeUserApi typeUserApi) {
        this.name = name;
        this.typeUserApi = typeUserApi;
    }

    public UserApiDTO(UserApi userApi) {
        this.name = userApi.getName();
        this.urlCallback = userApi.getUrlCallback();
        this.id = userApi.getId();
        this.name2 = userApi.getName2();
        this.site = userApi.getSite();
        this.textModal = userApi.getTextModal();
        this.partnerApiKeyCallback = userApi.getPartnerApiKeyCallback();
    }

}
