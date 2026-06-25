package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.application.exception.UnauthorizedException;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAccessPolicyTest {

    private TenantAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TenantAccessPolicy();
    }

    @Test
    void should_throw_exception_when_current_tenant_apartment_sharing_mismatch() {
        // Given
        Tenant currentTenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        Tenant targetTenant = new Tenant(TenantEntity.builder().id(2L).apartmentSharingId(20L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(20L).build());

        // When & Then
        assertThatThrownBy(() -> policy.validateAccess(currentTenant, targetTenant, apartmentSharing))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not have access to apartment sharing with id : 20");
    }

    @Test
    void should_allow_access_when_editing_own_folder() {
        // Given
        Tenant currentTenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        // When & Then
        assertThatCode(() -> policy.validateAccess(currentTenant, currentTenant, apartmentSharing))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_exception_when_target_tenant_apartment_sharing_mismatch() {
        // Given
        Tenant currentTenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        Tenant targetTenant = new Tenant(TenantEntity.builder().id(2L).apartmentSharingId(20L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        // When & Then
        assertThatThrownBy(() -> policy.validateAccess(currentTenant, targetTenant, apartmentSharing))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not have access to tenant with id : 2");
    }

    @Test
    void should_throw_exception_when_group_tenant_edits_another_tenant() {
        // Given
        Tenant currentTenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        Tenant targetTenant = new Tenant(TenantEntity.builder().id(2L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(
                ApartmentSharingEntity.builder().id(10L).applicationType(ApplicationType.GROUP).build()
        );

        // When & Then
        assertThatThrownBy(() -> policy.validateAccess(currentTenant, targetTenant, apartmentSharing))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not have access to tenant with id : 2");
    }

    @Test
    void should_allow_access_when_couple_member_edits_partner() {
        // Given
        Tenant currentTenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        Tenant targetTenant = new Tenant(TenantEntity.builder().id(2L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(
                ApartmentSharingEntity.builder().id(10L).applicationType(ApplicationType.COUPLE).build()
        );

        // When & Then
        assertThatCode(() -> policy.validateAccess(currentTenant, targetTenant, apartmentSharing))
                .doesNotThrowAnyException();
    }
}
