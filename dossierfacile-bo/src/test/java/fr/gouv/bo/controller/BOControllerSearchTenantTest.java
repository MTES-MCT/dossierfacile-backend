package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BOControllerSearchTenantTest {

    private TenantService tenantService;
    private BOApplicationAccessService applicationAccessService;
    private BOController controller;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        applicationAccessService = mock(BOApplicationAccessService.class);
        controller = new BOController(
                tenantService,
                null,
                null,
                null,
                null,
                applicationAccessService
        );
    }

    @Test
    void searchTenant_logsSearchAndRedirectsWhenSingleEmailMatch() {
        UserPrincipal principal = supportPrincipal();
        Tenant tenant = tenant(42L, 99L);
        Page<Tenant> page = new PageImpl<>(List.of(tenant), PageRequest.of(0, 20), 1);
        when(tenantService.getTenantByIdOrEmail(eq("john.doe@example.com"), any(PageRequest.class))).thenReturn(page);

        String view = controller.searchTenant(new ExtendedModelMap(), principal, "john.doe@example.com", 1);

        assertThat(view).isEqualTo("redirect:/bo/colocation/99");
        verify(applicationAccessService).checkAndLogSearchTenant(principal, "john.doe@example.com", 1L);
    }

    @Test
    void searchTenant_logsSearchWithNullAnchorWhenNoResult() {
        UserPrincipal principal = supportPrincipal();
        Page<Tenant> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(tenantService.getTenantByIdOrEmail(eq("nobody@example.com"), any(PageRequest.class))).thenReturn(page);

        String view = controller.searchTenant(new ExtendedModelMap(), principal, "nobody@example.com", 1);

        assertThat(view).isEqualTo("bo/search");
        verify(applicationAccessService).checkAndLogSearchTenant(principal, "nobody@example.com", 0L);
    }

    private Tenant tenant(Long tenantId, Long apartmentSharingId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTenantType(TenantType.CREATE);
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(apartmentSharingId);
        tenant.setApartmentSharing(apartmentSharing);
        return tenant;
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(
                20L,
                "support",
                "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT"))
        );
    }
}
