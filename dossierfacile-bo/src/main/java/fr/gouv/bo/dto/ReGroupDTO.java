package fr.gouv.bo.dto;


import fr.dossierfacile.common.enums.ApplicationType;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReGroupDTO {
    @Email
    private String tenantEmailCreate;
    @Email
    private String tenantEmailJoin;
    private ApplicationType applicationType;
}
