package fr.gouv.owner.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantSituation;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.owner.annotation.JoinApartmentSharingByMail;
import fr.gouv.owner.annotation.JoinApartmentSharingCapacity;
import fr.gouv.owner.validator.interfaces.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;


import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@ApiModel(description = "All details about tenant")
public class TenantDTO {

    @JsonIgnore
    public String tmpDir;
    @JsonIgnore
    private Long id;

    @ApiModelProperty(notes = "first name of tenant")
    private String firstName;

    @ApiModelProperty(notes = "last name of tenant")
    private String lastName;

    @NotNull(groups = {TenantCreateApartmentSharing.class, TenantJoinApartmentSharing.class, TenantAlone.class, CreateTenantRestApi.class, UpdateTenantRestApi.class})
    @ApiModelProperty(notes = "situation of tenant")
    private Integer situation;
    @NotNull(groups = {TenantCreateApartmentSharing.class, TenantJoinApartmentSharing.class, TenantAlone.class, CreateTenantRestApi.class, UpdateTenantRestApi.class})
    @ApiModelProperty(notes = "salary of tenant")
    private Integer salary;

    @ApiModelProperty(notes = "email of tenant")
    private String email;
    @JsonIgnore
    private Integer satisfactionSurvey;
    @JsonIgnore
    private MultipartFile[][] files;

    @JsonIgnore
    private String password;
    @Range(min = 2, max = 6, groups = {TenantCreateApartmentSharing.class})
    @JsonIgnore
    private Integer numberOfTenants;
    @JsonIgnore

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
    @ApiModelProperty(notes = "guarantor of tenant")
    private GuarantorDTO guarantor;
    @JsonIgnore
    private String[] path;

    @JsonIgnore
    private Boolean acceptVerificationGuarantorType;

    @JsonIgnore
    private String slug;

    @JsonIgnore
    private Boolean acceptVerification;

    @JsonIgnore
    private Boolean declarationHonor;

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

    private boolean acceptSharingInfoPartner;
    @JsonIgnore

    private boolean couple = false;
    @JsonIgnore
    private boolean pension = false;
    @JsonIgnore
    private boolean otherSalaries = false;
    @JsonIgnore

    private int pensionNumber = 1;

    public TenantDTO(Tenant tenant) {
        this.id = tenant.getId();
        this.firstName = tenant.getFirstName();
        this.lastName = tenant.getLastName();
        this.salary = tenant.getTotalSalary();
        this.situation = tenant.getTenantSituation() == TenantSituation.UNDEFINED ? TenantSituation.CDD.ordinal() : tenant.getTenantSituation().ordinal();
        this.email = tenant.getEmail();

        this.tenantType = tenant.getTenantType();
    }
}
