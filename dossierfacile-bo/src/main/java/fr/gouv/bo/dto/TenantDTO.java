package fr.gouv.bo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantSituation;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.gouv.bo.validator.annotation.AcceptVerification;
import fr.gouv.bo.validator.annotation.JoinApartmentSharingByMail;
import fr.gouv.bo.validator.annotation.JoinApartmentSharingCapacity;
import fr.gouv.bo.validator.annotation.UniqueEmail;
import fr.gouv.bo.validator.interfaces.CreateTenantRestApi;
import fr.gouv.bo.validator.interfaces.Step1;
import fr.gouv.bo.validator.interfaces.Step1LightApi;
import fr.gouv.bo.validator.interfaces.Step2;
import fr.gouv.bo.validator.interfaces.Step3;
import fr.gouv.bo.validator.interfaces.Step5;
import fr.gouv.bo.validator.interfaces.Step5File5;
import fr.gouv.bo.validator.interfaces.Step5File51;
import fr.gouv.bo.validator.interfaces.Step9;
import fr.gouv.bo.validator.interfaces.TenantAlone;
import fr.gouv.bo.validator.interfaces.TenantCreateApartmentSharing;
import fr.gouv.bo.validator.interfaces.TenantJoinApartmentSharing;
import fr.gouv.bo.validator.interfaces.TenantJoinApartmentSharingBO;
import fr.gouv.bo.validator.interfaces.UpdateTenantRestApi;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;


@Getter
@Setter
@AllArgsConstructor
@ApiModel(description = "All details about tenant")
public class TenantDTO {

    @JsonIgnore
    public String tmpDir;
    @JsonIgnore
    private Long id;

    @Pattern(regexp = "^((?!�).)*$", message = "caractère non autorisé", groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class, UpdateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @NotBlank(groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class, UpdateTenantRestApi.class})
    @Size(min = 1, max = 50, groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @ApiModelProperty(notes = "first name of tenant")
    private String firstName;

    @Pattern(regexp = "^((?!�).)*$", message = "caractère non autorisé", groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class, UpdateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @NotBlank(groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class, UpdateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @Size(min = 1, max = 50, groups = {Step1.class, Step1LightApi.class, CreateTenantRestApi.class})
    @ApiModelProperty(notes = "last name of tenant")
    private String lastName;

    @NotNull(groups = {TenantCreateApartmentSharing.class, TenantJoinApartmentSharing.class, TenantAlone.class, CreateTenantRestApi.class, UpdateTenantRestApi.class})
    @ApiModelProperty(notes = "situation of tenant")
    private Integer situation;
    @NotNull(groups = {TenantCreateApartmentSharing.class, TenantJoinApartmentSharing.class, TenantAlone.class, CreateTenantRestApi.class, UpdateTenantRestApi.class})
    @ApiModelProperty(notes = "salary of tenant")
    private Integer salary;
    @NotBlank(groups = {Step2.class, CreateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @Email(groups = {Step2.class, CreateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @UniqueEmail(message = "Cette adresse email est déjà utilisée", groups = {Step2.class, CreateTenantRestApi.class, TenantJoinApartmentSharingBO.class})
    @ApiModelProperty(notes = "email of tenant")
    private String email;
    @JsonIgnore
    private Integer satisfactionSurvey;
    @JsonIgnore
    private MultipartFile[][] files;

    @NotBlank(groups = {Step2.class})
    @NotEmpty(groups = {Step2.class})
    @JsonIgnore
    private String password;
    @JsonIgnore
    private Integer numberOfTenants;
    @JsonIgnore
    @NotNull(groups = {Step3.class})
    private TenantType tenantType;
    @JsonIgnore
    @Email(groups = {TenantJoinApartmentSharing.class})
    @JoinApartmentSharingByMail(message = "Cette adresse email n'a pas été utilisée pour créer une colocation", groups = {TenantJoinApartmentSharing.class})
    @JoinApartmentSharingCapacity(message = "No more capacity in this colocation", groups = {TenantJoinApartmentSharing.class})
    private String tenantEmail;
    @JsonIgnore
    private UserApi partnerId;
    @JsonIgnore
    private String internalPartnerId;
    @JsonIgnore
    private String[] path;
    @NotNull(groups = {CreateTenantRestApi.class})
    @ApiModelProperty(notes = "guarantor type of tenant")
    private TypeGuarantor typeGuarantor;
    @AcceptVerification(groups = Step5.class, message = "Veuillez cocher la case ci-dessus pour continuer")
    @JsonIgnore
    private Boolean acceptVerificationGuarantorType;
    @JsonIgnore
    private String slug;
    @AcceptVerification(groups = Step5.class, message = "Veuillez cocher la case ci-dessus pour continuer")
    @JsonIgnore
    private Boolean acceptVerification;
    @AcceptVerification(groups = Step9.class, message = "Veuillez cocher la case ci-dessus pour continuer")
    @JsonIgnore
    private Boolean declarationHonor;
    @AcceptVerification(groups = Step9.class, message = "Veuillez cocher la case ci-dessus pour continuer")
    @JsonIgnore
    private Boolean declarationHonorColocs;
    @NotNull(groups = {CreateTenantRestApi.class})
    private boolean file4Generated = false;
    @JsonIgnore
    private boolean file5Generated = false;
    @JsonIgnore
    private String folder;
    @JsonIgnore
    private String telephone;
    @JsonIgnore
    private String file5Text;
    @JsonIgnore
    private String file4Text;
    @AssertTrue(groups = Step1LightApi.class)
    private boolean acceptSharingInfoPartner;

    private boolean couple = false;
    @JsonIgnore
    private boolean pension = false;
    @JsonIgnore
    private boolean otherSalaries = false;
    @JsonIgnore
    @Min(value = 1, groups = Step5File51.class)
    private int pensionNumber = 1;


    public TenantDTO() {
        this.situation = TenantSituation.CDD.ordinal();
        this.salary = 0;
        this.tenantType = TenantType.CREATE;
        this.file4Generated = false;
    }

    @AssertTrue(groups = {Step5File5.class}, message = "you must mark at least one cause")
    private boolean isOneCheck() {
        return pension || otherSalaries;
    }
}
