package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.repository.UserApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserApiServiceImplTest {

    private UserApiService userApiService;
    private UserApiRepository userApiRepository;
    private TenantUserApiRepository tenantUserApiRepository;

    @BeforeEach
    void setUp() {
        userApiRepository = mock(UserApiRepository.class);
        tenantUserApiRepository = mock(TenantUserApiRepository.class);
        userApiService = new UserApiServiceImpl(userApiRepository, tenantUserApiRepository);
    }

    @Test
    void shouldReturnTrueWhenPartnerIsLinkedToAtLeastOneTenant() {
        UserApi partner = UserApi.builder().id(1L).name("partner").build();
        Tenant tenant = Tenant.builder().id(100L).build();
        TenantUserApi tenantUserApi = TenantUserApi.builder()
                .tenant(tenant)
                .userApi(partner)
                .build();

        when(tenantUserApiRepository.findFirstByUserApiAndTenantIn(eq(partner), any())).thenReturn(Optional.of(tenantUserApi));

        boolean result = userApiService.anyTenantIsLinked(partner, List.of(tenant));

        assertTrue(result);
        verify(tenantUserApiRepository, times(1)).findFirstByUserApiAndTenantIn(partner, List.of(tenant));
    }

    @Test
    void shouldReturnFalseWhenPartnerIsNotLinkedToAnyTenant() {
        UserApi partner = UserApi.builder().id(1L).name("partner").build();
        Tenant tenant = Tenant.builder().id(100L).build();

        when(tenantUserApiRepository.findFirstByUserApiAndTenantIn(eq(partner), any())).thenReturn(Optional.empty());

        boolean result = userApiService.anyTenantIsLinked(partner, List.of(tenant));

        assertFalse(result);
        verify(tenantUserApiRepository, times(1)).findFirstByUserApiAndTenantIn(partner, List.of(tenant));
    }
}
