package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.AccountForm;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class AccountApiPartner implements SaveStep<AccountForm> {

    private final TenantRepository tenantRepository;
    private final ApartmentSharingService apartmentSharingService;
    private final TenantMapper tenantMapper;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public TenantModel saveStep(Tenant t, AccountForm accountForm) {
        String email = accountForm.getEmail().toLowerCase();
        Tenant tenant = tenantRepository.findByEmailAndEnabledFalse(email).orElse(new Tenant(email));
        tenant.setEnabled(true);
        tenant.addLinkedKeycloakClient(authenticationFacade.getKeycloakClientId());
        tenantRepository.save(tenant);
        apartmentSharingService.createApartmentSharing(tenant);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), null);
        return tenantMapper.toTenantModel(tenantRepository.save(tenant));
    }
}
