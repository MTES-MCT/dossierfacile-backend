package fr.gouv.owner.dto;

import fr.dossierfacile.common.entity.Prospect;
import fr.dossierfacile.common.enums.TenantSituation;
import fr.gouv.owner.validator.interfaces.CreateProspect;
import fr.gouv.owner.annotation.UniqueEmail;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.Email;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProspectDTO {
    private Long id;
    private String firstName;
    private String lastName;
    @Email(groups = {CreateProspect.class})
    @UniqueEmail(message = "Cette adresse email est déjà utilisée", groups = {CreateProspect.class})
    private String email;
    private String phone;
    private Long propertyId;
    @ApiModelProperty(notes = "situation of prospect")
    private Integer situation;
    private String guarantor;
    @ApiModelProperty(notes = "salary of prospect")
    private Integer salary;
    private boolean booking;
    private String customMessage;

    // TODO: implement validators for this
//    @UniqueEmailList(message = "Cette adresse email est déjà utilisée")
//    @DistinctEmailList(message = "Les emails doivent être différents")
//    @NotNullFirstNameList(message = "Le nom ne peut pas être vide")
//    @NotNullLastNameList(message = "Le nom de famille ne peut pas être vide")
    private List<CoProspectDTO> coProspects = new ArrayList<>();

    public ProspectDTO(Prospect prospect) {
        this.id = prospect.getId();
        this.firstName = prospect.getFirstName();
        this.lastName = prospect.getLastName();
        this.email = prospect.getEmail();
        this.phone = prospect.getPhone();
        if (prospect.getProperty() != null) {
            this.propertyId = prospect.getProperty().getId();
        }
        if (prospect.getTenantSituation() != null) {
            this.situation = prospect.getTenantSituation().ordinal();
        } else {
            this.situation = TenantSituation.CDD.ordinal();
        }
        this.guarantor = prospect.getGuarantor();
        this.salary = prospect.getSalary();
        this.booking = false;
    }

    public void addProspect(CoProspectDTO coProspectDTO) {
        this.coProspects.add(coProspectDTO);
    }
}
