package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuarantorServiceImplTest {

    @InjectMocks
    private GuarantorServiceImpl guarantorService;

    @Mock
    private GuarantorRepository guarantorRepository;

    @Mock
    private fr.dossierfacile.api.front.service.interfaces.TenantStatusService tenantStatusService;

    @Mock
    private fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService apartmentSharingService;

    @Nested
    class Delete {

        @Nested
        class WhenGroupTenantTriesToDeleteCoTenantGuarantor {
            @Test
            void shouldThrowAccessDeniedException() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.GROUP);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Guarantor guarantor = Guarantor.builder()
                        .id(1L)
                        .tenant(tenant2)
                        .build();

                when(guarantorRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(guarantor));

                assertThatThrownBy(() -> guarantorService.delete(1L, tenant1))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessageContaining("Not authorized to delete this guarantor");
            }
        }

        @Nested
        class WhenGroupTenantDeletesOwnGuarantor {
            @Test
            void shouldSucceed() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.GROUP);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Guarantor guarantor = Guarantor.builder()
                        .id(1L)
                        .tenant(tenant1)
                        .build();

                when(guarantorRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(guarantor));

                assertThatCode(() -> guarantorService.delete(1L, tenant1)).doesNotThrowAnyException();
                verify(guarantorRepository).delete(guarantor);
            }
        }

        @Nested
        class WhenCoupleTenantDeletesCoTenantGuarantor {
            @Test
            void shouldSucceed() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.COUPLE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Guarantor guarantor = Guarantor.builder()
                        .id(1L)
                        .tenant(tenant2)
                        .build();

                when(guarantorRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(guarantor));

                assertThatCode(() -> guarantorService.delete(1L, tenant1)).doesNotThrowAnyException();
                verify(guarantorRepository).delete(guarantor);
            }
        }

        @Nested
        class WhenGuarantorNotFound {
            @Test
            void shouldThrowGuarantorNotFoundException() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                Tenant tenant = Tenant.builder().id(1L).apartmentSharing(sharing).build();

                when(guarantorRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> guarantorService.delete(1L, tenant))
                        .isInstanceOf(GuarantorNotFoundException.class);
            }
        }
    }
}
