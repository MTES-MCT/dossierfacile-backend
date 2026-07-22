package fr.dossierfacile.api.front.domain.service;

import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantSynchronizationDomainServiceTest {

    @Mock
    private TenantCommonRepository tenantRepository;

    @Mock
    private TenantStatusService tenantStatusService;

    @Mock
    private LogService logService;

    @Mock
    private DocumentService documentService;

    private TenantSynchronizationDomainService syncService;

    @BeforeEach
    void setUp() {
        syncService = new TenantSynchronizationDomainService(
                tenantRepository,
                tenantStatusService,
                logService,
                documentService
        );
    }

    @Test
    void shouldNotUpdateTenantWhenMatches() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakId("kc-1")
                .email("test@test.fr")
                .firstName("John")
                .lastName("Doe")
                .franceConnect(false)
                .build();

        KeycloakUser keycloakUser = KeycloakUser.builder()
                .keycloakId("kc-1")
                .email("test@test.fr")
                .givenName("John")
                .familyName("Doe")
                .franceConnect(false)
                .build();

        Tenant result = syncService.synchronizeTenant(tenant, keycloakUser);

        verify(tenantRepository, never()).saveAndFlush(any());
        assertThat(result).isEqualTo(tenant);
    }

    @Test
    void shouldSetWarningWhenEmailMismatch() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakId("kc-1")
                .email("old@test.fr")
                .build();

        KeycloakUser keycloakUser = KeycloakUser.builder()
                .keycloakId("kc-1")
                .email("new@test.fr")
                .build();

        Tenant result = syncService.synchronizeTenant(tenant, keycloakUser);

        verify(tenantRepository, never()).saveAndFlush(any());
        assertThat(result.getWarningMessage()).isNotBlank();
    }

    @Test
    void shouldSynchronizeAndLogWhenLinkingFranceConnect() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakId("kc-1")
                .email("test@test.fr")
                .franceConnect(false)
                .ownerType(TenantOwnerType.SELF)
                .documents(new ArrayList<>())
                .build();

        KeycloakUser keycloakUser = KeycloakUser.builder()
                .keycloakId("kc-1")
                .email("test@test.fr")
                .franceConnect(true)
                .franceConnectSub("fc-sub-123")
                .givenName("Jean")
                .familyName("Dupont")
                .build();

        when(tenantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = syncService.synchronizeTenant(tenant, keycloakUser);

        verify(logService).saveLog(LogType.FC_ACCOUNT_LINK, 1L);
        verify(tenantStatusService).updateTenantStatus(tenant);
        verify(tenantRepository).saveAndFlush(tenant);

        assertThat(Boolean.TRUE.equals(result.getFranceConnect())).isTrue();
        assertThat(result.getFranceConnectSub()).isEqualTo("fc-sub-123");
        assertThat(result.getUserFirstName()).isEqualTo("Jean");
        assertThat(result.getUserLastName()).isEqualTo("Dupont");
        assertThat(result.getFcHash()).isEqualTo("de6942fe6d4b0427d3b47c136fef3ad43f6d36b70460e9422190aac9e2c5de39");
    }

    @Test
    void shouldUpdateTenantWhenFcHashesDiffer() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .keycloakId("kc-1")
                .email("test@test.fr")
                .firstName("Jean")
                .lastName("Dupont")
                .franceConnect(true)
                .fcHash("old-hash")
                .ownerType(TenantOwnerType.SELF)
                .documents(new ArrayList<>())
                .build();

        KeycloakUser keycloakUser = KeycloakUser.builder()
                .keycloakId("kc-1")
                .email("test@test.fr")
                .franceConnect(true)
                .franceConnectSub("fc-sub-123")
                .givenName("Jean")
                .familyName("Dupont")
                .build();

        when(tenantRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = syncService.synchronizeTenant(tenant, keycloakUser);

        verify(tenantRepository).saveAndFlush(tenant);
        assertThat(result.getFcHash()).isEqualTo("de6942fe6d4b0427d3b47c136fef3ad43f6d36b70460e9422190aac9e2c5de39");
    }
}
