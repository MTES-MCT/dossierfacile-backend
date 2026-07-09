package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.exception.ApplicationRegistrationException;
import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationRegistrationValidatorTest {

    @Mock
    private TenantCommonRepository tenantRepository;

    @InjectMocks
    private ApplicationRegistrationValidator validator;

    private void assertRejectedWith(Tenant tenant, ApplicationFormV2 form, ApplicationErrorCode expectedCode) {
        assertThatThrownBy(() -> validator.validate(tenant, form))
                .isInstanceOfSatisfying(ApplicationRegistrationException.class,
                        e -> assertThat(e.getCode()).isEqualTo(expectedCode));
    }

    private void assertAccepted(Tenant tenant, ApplicationFormV2 form) {
        assertThatCode(() -> validator.validate(tenant, form)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptCoupleFormWithNewEmail() {
        assertAccepted(createMainTenant(), coupleForm("spouse@example.com"));
    }

    @Test
    void shouldAcceptAloneFormWithoutQueryingRepository() {
        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.ALONE)
                .coTenants(Collections.emptyList())
                .build();

        assertAccepted(createMainTenant(), form);
        verify(tenantRepository, never()).existsByEmail(anyString());
    }

    @Test
    void shouldAcceptNullCoTenants() {
        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.ALONE)
                .coTenants(null)
                .build();

        assertAccepted(createMainTenant(), form);
    }

    @Test
    void shouldRejectJoinTenantBeforeOtherRules() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .tenantType(TenantType.JOIN)
                .apartmentSharing(ApartmentSharing.builder().build())
                .build();

        assertRejectedWith(tenant, coupleForm("spouse@example.com"), ApplicationErrorCode.APPLICATION_TYPE_DENIED_FOR_JOIN);
        verify(tenantRepository, never()).existsByEmail(anyString());
    }

    @Test
    void shouldRejectEmailAlreadyUsedByAnotherAccount() {
        when(tenantRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertRejectedWith(createMainTenant(), coupleForm("taken@example.com"), ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
    }

    @Test
    void shouldRejectOwnEmailUsedAsCoTenantEmail() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
        Tenant mainTenant = Tenant.builder()
                .id(1L)
                .email("principal@example.com")
                .tenantType(TenantType.CREATE)
                .apartmentSharing(apartmentSharing)
                .build();
        apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant)));

        when(tenantRepository.existsByEmail("principal@example.com")).thenReturn(true);

        assertRejectedWith(mainTenant, coupleForm("principal@example.com"), ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
    }

    @Test
    void shouldAllowUnchangedCoTenantEmail() {
        Tenant mainTenant = createMainTenantWithCoTenant("Louise", "Martin", "spouse@example.com");

        assertAccepted(mainTenant, coupleForm("spouse@example.com"));
        verify(tenantRepository, never()).existsByEmail(anyString());
    }

    @Test
    void shouldAllowUnchangedCoTenantEmailRegardlessOfCase() {
        Tenant mainTenant = createMainTenantWithCoTenant("Louise", "Martin", "Spouse@Example.COM");

        assertAccepted(mainTenant, coupleForm("spouse@example.com"));
    }

    @Test
    void shouldRejectNewEmailForExistingCoTenantWhenEmailAlreadyTaken() {
        Tenant mainTenant = createMainTenantWithCoTenant("Louise", "Martin", null);

        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.COUPLE)
                .acceptAccess(true)
                .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "NEW@example.com")))
                .build();

        when(tenantRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertRejectedWith(mainTenant, form, ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
    }

    // Co-tenants are matched on email alone: names may be corrected or completed
    // (e.g. legacy dossiers where the co-tenant was invited by email only).
    @Test
    void shouldAllowRenamedCoTenantKeepingSameEmail() {
        Tenant mainTenant = createMainTenantWithCoTenant("Louise", "Martin", "spouse@example.com");

        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.COUPLE)
                .acceptAccess(true)
                .coTenants(List.of(new CoTenantForm("Louisa", "Martin", null, "spouse@example.com")))
                .build();

        assertAccepted(mainTenant, form);
        verify(tenantRepository, never()).existsByEmail(anyString());
    }

    @Test
    void shouldAllowCompletingNamesOfExistingCoTenantWithoutNames() {
        Tenant mainTenant = createMainTenantWithCoTenant(null, null, "spouse@example.com");

        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.COUPLE)
                .acceptAccess(true)
                .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                .build();

        assertAccepted(mainTenant, form);
        verify(tenantRepository, never()).existsByEmail(anyString());
    }

    @Test
    void shouldCheckEachGroupCoTenantEmailIndependently() {
        ApplicationFormV2 form = ApplicationFormV2.builder()
                .applicationType(ApplicationType.GROUP)
                .acceptAccess(true)
                .coTenants(List.of(
                        new CoTenantForm("first", "one", null, "free@example.com"),
                        new CoTenantForm("second", "two", null, "taken@example.com")
                ))
                .build();

        when(tenantRepository.existsByEmail("free@example.com")).thenReturn(false);
        when(tenantRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertRejectedWith(createMainTenant(), form, ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
    }

    private static Tenant createMainTenant() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
        Tenant tenant = Tenant.builder()
                .id(1L)
                .tenantType(TenantType.CREATE)
                .apartmentSharing(apartmentSharing)
                .build();
        apartmentSharing.setTenants(new ArrayList<>(List.of(tenant)));
        return tenant;
    }

    private static Tenant createMainTenantWithCoTenant(String firstName, String lastName, String email) {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
        Tenant coTenant = Tenant.builder()
                .id(2L)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .build();
        Tenant mainTenant = Tenant.builder()
                .id(1L)
                .tenantType(TenantType.CREATE)
                .apartmentSharing(apartmentSharing)
                .build();
        apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant, coTenant)));
        return mainTenant;
    }

    private static ApplicationFormV2 coupleForm(String email) {
        return ApplicationFormV2.builder()
                .applicationType(ApplicationType.COUPLE)
                .acceptAccess(true)
                .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, email)))
                .build();
    }
}
