package fr.dossierfacile.common.dto.mail;

import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentSharingDto {
    private List<TenantDto> tenants;
    private ApplicationType applicationType;
}
