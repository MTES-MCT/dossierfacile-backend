package fr.dossierfacile.api.front.domain.service;

import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindOrCreateTenantDomainServiceTest {

    @Mock
    private TenantCommonRepository tenantRepository;

    @Mock
    private TenantService tenantService;

    private FindOrCreateTenantDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new FindOrCreateTenantDomainService(tenantRepository, tenantService);
    }

    @Test
    void shouldReturnTenantByKeycloakIdWhenExists() {
        KeycloakUser user = KeycloakUser.builder().keycloakId("kc-123").email("user@test.fr").build();
        Tenant existing = Tenant.builder().id(1L).keycloakId("kc-123").build();

        when(tenantRepository.findByKeycloakId("kc-123")).thenReturn(existing);

        Tenant result = domainService.findOrCreateTenant(user, null);

        assertThat(result).isEqualTo(existing);
        verify(tenantRepository, times(1)).findByKeycloakId("kc-123");
        verify(tenantRepository, never()).findByEmail(any());
        verify(tenantService, never()).registerFromKeycloakUser(any(), any(), any());
    }

    @Test
    void shouldReturnTenantByEmailWhenKeycloakIdNotAssociated() {
        KeycloakUser user = KeycloakUser.builder().keycloakId("kc-123").email("user@test.fr").build();
        Tenant existing = Tenant.builder().id(2L).email("user@test.fr").build();

        when(tenantRepository.findByKeycloakId("kc-123")).thenReturn(null);
        when(tenantRepository.findByEmail("user@test.fr")).thenReturn(Optional.of(existing));

        Tenant result = domainService.findOrCreateTenant(user, null);

        assertThat(result).isEqualTo(existing);
        verify(tenantService, never()).registerFromKeycloakUser(any(), any(), any());
    }

    @Test
    void shouldRegisterNewTenantWhenNotFound() {
        KeycloakUser user = KeycloakUser.builder().keycloakId("kc-123").email("user@test.fr").build();
        Tenant created = Tenant.builder().id(3L).keycloakId("kc-123").email("user@test.fr").build();

        when(tenantRepository.findByKeycloakId("kc-123")).thenReturn(null);
        when(tenantRepository.findByEmail("user@test.fr")).thenReturn(Optional.empty());
        when(tenantService.registerFromKeycloakUser(user, null, null)).thenReturn(created);

        Tenant result = domainService.findOrCreateTenant(user, null);

        assertThat(result).isEqualTo(created);
        verify(tenantService, times(1)).registerFromKeycloakUser(user, null, null);
    }

    @Test
    void shouldHandleRaceConditionGracefully() {
        KeycloakUser user = KeycloakUser.builder().keycloakId("kc-123").email("user@test.fr").build();
        Tenant winnerTenant = Tenant.builder().id(4L).keycloakId("kc-123").build();

        when(tenantRepository.findByKeycloakId("kc-123")).thenReturn(null, winnerTenant);
        when(tenantRepository.findByEmail("user@test.fr")).thenReturn(Optional.empty());
        when(tenantService.registerFromKeycloakUser(user, null, null))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        Tenant result = domainService.findOrCreateTenant(user, null);

        assertThat(result).isEqualTo(winnerTenant);
    }
}
