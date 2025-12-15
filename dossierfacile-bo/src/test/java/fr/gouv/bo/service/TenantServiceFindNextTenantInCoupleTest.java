package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantServiceFindNextTenantInCoupleTest {

    private static final Long CURRENT_TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final Long APARTMENT_SHARING_ID = 100L;

    private TenantCommonRepository tenantRepository;
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantCommonRepository.class);
        tenantService = new TenantService(
                tenantRepository,
                null, // mailService
                null, // partnerCallBackService
                null, // userService
                null, // messageSource
                null, // documentRepository
                null, // documentDeniedReasonsRepository
                null, // messageService
                null, // apartmentSharingRepository
                null, // operatorLogRepository
                null, // documentDeniedReasonsService
                null, // documentService
                null, // tenantLogService
                null, // keycloakService
                null, // apartmentSharingService
                null, // guarantorRepository
                null, // tenantMapperForMail
                null, // apartmentSharingMapperForMail
                null, // tenantCommonService
                null  // tenantLogCommonService
        );
    }

    @Nested
    class FindNextTenantInCouple {

        @Test
        void should_throw_npe_when_tenant_is_null() {
            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(null);

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_apartment_sharing_is_null() {
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(null)
                    .build();
            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_application_type_is_alone() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.ALONE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .build();
            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_application_type_is_group() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.GROUP)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .build();
            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_next_tenant_when_couple_has_another_tenant_in_to_process() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.COUPLE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.VALIDATED)
                    .build();
            Tenant otherTenant = Tenant.builder()
                    .id(OTHER_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.TO_PROCESS)
                    .build();

            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(currentTenant, otherTenant));

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(OTHER_TENANT_ID);
        }

        @Test
        void should_return_empty_when_no_other_tenant_in_to_process() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.COUPLE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.TO_PROCESS)
                    .build();
            Tenant otherTenant = Tenant.builder()
                    .id(OTHER_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.VALIDATED)
                    .build();

            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(currentTenant, otherTenant));

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_other_tenant_status_is_declined() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.COUPLE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.TO_PROCESS)
                    .build();
            Tenant otherTenant = Tenant.builder()
                    .id(OTHER_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.DECLINED)
                    .build();

            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(currentTenant, otherTenant));

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_other_tenant_status_is_incomplete() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.COUPLE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.TO_PROCESS)
                    .build();
            Tenant otherTenant = Tenant.builder()
                    .id(OTHER_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.INCOMPLETE)
                    .build();

            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(currentTenant, otherTenant));

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_only_current_tenant_exists() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .applicationType(ApplicationType.COUPLE)
                    .build();
            Tenant currentTenant = Tenant.builder()
                    .id(CURRENT_TENANT_ID)
                    .apartmentSharing(apartmentSharing)
                    .status(TenantFileStatus.TO_PROCESS)
                    .build();

            when(tenantRepository.findOneById(CURRENT_TENANT_ID)).thenReturn(currentTenant);
            when(tenantRepository.findAllByApartmentSharingId(APARTMENT_SHARING_ID))
                    .thenReturn(List.of(currentTenant));

            Optional<Tenant> result = tenantService.findNextTenantInCouple(CURRENT_TENANT_ID);

            assertThat(result).isEmpty();
        }
    }
}
