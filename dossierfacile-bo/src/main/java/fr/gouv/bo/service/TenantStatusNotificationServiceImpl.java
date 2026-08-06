package fr.gouv.bo.service;

import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.mapper.mail.ApartmentSharingMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.service.interfaces.TenantStatusNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class TenantStatusNotificationServiceImpl implements TenantStatusNotificationService {

    private final MailService mailService;
    private final TenantMapperForMail tenantMapperForMail;
    private final ApartmentSharingMapperForMail apartmentSharingMapperForMail;

    @Override
    public void notifyTenantDeclined(Tenant tenant) {
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        ApartmentSharingDto apartmentSharingDto = apartmentSharingMapperForMail.toDto(tenant.getApartmentSharing());

        if (apartmentSharingDto.getApplicationType() == ApplicationType.COUPLE) {
            apartmentSharingDto.getTenants().stream()
                    .filter(t -> isNotBlank(t.getEmail()))
                    .forEach(t -> mailService.sendEmailToTenantAfterTenantDenied(t, tenantDto, null));
        } else {
            mailService.sendMailNotificationAfterDeny(tenantDto, null);
        }
    }

    @Override
    public void notifyTenantValidated(Tenant tenant) {
        // Logique spécifique si besoin (ex: Mail de validation). Historiquement vide pour OperatorDeleteFileUseCase.
    }
}
