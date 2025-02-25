package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class TenantPermissionsServiceImplTest {

    private TenantPermissionsServiceImpl tenantPermissionsService;
    private TenantService tenantService;
    private TenantUserApiRepository tenantUserApiRepository;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        tenantUserApiRepository = mock(TenantUserApiRepository.class);
        tenantPermissionsService = new TenantPermissionsServiceImpl(tenantService, tenantUserApiRepository);
    }

    @Test
    void shouldReturnTrueWhenTenantExistWithUserApi() {
        String keycloakClientId = "client_id";
        Long tenantId = 1L;

        when(tenantUserApiRepository.existsByUserApiNameAndTenantId(keycloakClientId, tenantId)).thenReturn(true);

        assertTrue(tenantPermissionsService.clientCanAccess(keycloakClientId, tenantId));

        verify(tenantUserApiRepository, times(1)).existsByUserApiNameAndTenantId(keycloakClientId, tenantId);
    }

    @Test
    void shouldReturnFalseWhenTenantNotExistWithUserApi() {
        String keycloakClientId = "client_id";
        Long tenantId = 1L;

        when(tenantUserApiRepository.existsByUserApiNameAndTenantId(keycloakClientId, tenantId)).thenReturn(false);

        assertFalse(tenantPermissionsService.clientCanAccess(keycloakClientId, tenantId));

        verify(tenantUserApiRepository, times(1)).existsByUserApiNameAndTenantId(keycloakClientId, tenantId);
    }

    @Test
    void shouldReturnTrueWhenTenantRequestNoTenant() {
        String keycloakUserId = "user_id";
        Long tenantId = 1L;

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(tenantService.findByKeycloakId(keycloakUserId)).thenReturn(tenant);

        assertTrue(tenantPermissionsService.canAccess(keycloakUserId, null));

        verify(tenantService, times(1)).findByKeycloakId(keycloakUserId);
    }

    @Test
    void shouldReturnTrueWhenTenantTryToAccessTheirOwnTenant() {
        String keycloakUserId = "user_id";
        Long tenantId = 1L;

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(tenantService.findByKeycloakId(keycloakUserId)).thenReturn(tenant);

        assertTrue(tenantPermissionsService.canAccess(keycloakUserId, tenantId));

        verify(tenantService, times(1)).findByKeycloakId(keycloakUserId);
    }

    @Test
    void shouldReturnTrueWhenCoupleTryToAccessTheirTenant() {
        String keycloakUserId = "user_id";
        Long tenantId = 1L;
        Long OtherTenantId = 2L;

        var apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.COUPLE)
                .build();

        Tenant tenant1 = new Tenant();
        tenant1.setId(tenantId);
        tenant1.setApartmentSharing(apartmentSharing);

        Tenant tenant2 = new Tenant();
        tenant2.setId(OtherTenantId);
        tenant2.setApartmentSharing(apartmentSharing);

        apartmentSharing.setTenants(List.of(tenant1, tenant2));

        when(tenantService.findByKeycloakId(keycloakUserId)).thenReturn(tenant2);

        assertTrue(tenantPermissionsService.canAccess(keycloakUserId, tenantId));

        verify(tenantService, times(1)).findByKeycloakId(keycloakUserId);
    }

    @Test
    void shouldReturnFalseWhenCoupleTryToAccessOtherTenant() {
        String keycloakUserId = "user_id";
        Long tenantId = 1L;
        Long OtherTenantId = 2L;

        var apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.COUPLE)
                .build();

        Tenant tenant1 = new Tenant();
        tenant1.setId(tenantId);
        tenant1.setApartmentSharing(apartmentSharing);

        Tenant tenant2 = new Tenant();
        tenant2.setId(OtherTenantId);
        tenant2.setApartmentSharing(apartmentSharing);

        apartmentSharing.setTenants(List.of(tenant1, tenant2));

        when(tenantService.findByKeycloakId(keycloakUserId)).thenReturn(tenant2);

        assertFalse(tenantPermissionsService.canAccess(keycloakUserId, 3L));

        verify(tenantService, times(1)).findByKeycloakId(keycloakUserId);
    }

    @Test
    void shouldReturnFalseTenantTryToAccessOtherTenant() {
        String keycloakUserId = "user_id";
        Long tenantId = 1L;
        Long otherTenantId = 2L;

        var apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.ALONE)
                .build();

        Tenant tenant1 = new Tenant();
        tenant1.setId(tenantId);
        tenant1.setApartmentSharing(apartmentSharing);

        Tenant tenant2 = new Tenant();
        tenant2.setId(otherTenantId);

        apartmentSharing.setTenants(List.of(tenant1));

        when(tenantService.findByKeycloakId(keycloakUserId)).thenReturn(tenant1);

        assertFalse(tenantPermissionsService.canAccess(keycloakUserId, otherTenantId));

        verify(tenantService, times(1)).findByKeycloakId(keycloakUserId);
    }
}