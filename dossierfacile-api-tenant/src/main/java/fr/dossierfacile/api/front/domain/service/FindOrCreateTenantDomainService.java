package fr.dossierfacile.api.front.domain.service;

import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindOrCreateTenantDomainService {

    private final TenantCommonRepository tenantRepository;
    private final TenantService tenantService;

    public Tenant findOrCreateTenant(KeycloakUser kcUser, AcquisitionData acquisitionData) {
        String keycloakId = kcUser.getKeycloakId();
        Tenant tenant = tenantRepository.findByKeycloakId(keycloakId);
        if (tenant != null) {
            return tenant;
        }
        log.warn("No tenant account found associated with keycloakId {}", keycloakId);
        Optional<Tenant> tenantByEmail = tenantRepository.findByEmail(kcUser.getEmail());
        if (tenantByEmail.isPresent()) {
            log.info("Found tenant by email from keycloak");
            return tenantByEmail.get();
        }
        log.info("Creating tenant account associated with keycloakId {}", keycloakId);
        try {
            return tenantService.registerFromKeycloakUser(kcUser, null, acquisitionData);
        } catch (DataIntegrityViolationException e) {
            log.info("Tenant account creation raced with a concurrent request, reusing existing account (keycloakId {})", keycloakId);
            return Optional.ofNullable(tenantRepository.findByKeycloakId(keycloakId))
                    .or(() -> tenantRepository.findByEmail(kcUser.getEmail()))
                    .orElseThrow(() -> e);
        }
    }
}
