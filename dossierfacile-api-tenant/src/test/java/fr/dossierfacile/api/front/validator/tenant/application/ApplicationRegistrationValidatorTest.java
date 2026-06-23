package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationRegistrationValidatorTest {

    @Mock
    private TenantCommonRepository tenantRepository;

    @InjectMocks
    private ApplicationRegistrationValidator validator;

    @Nested
    class HasValidStructure {

        @Test
        void shouldRejectNullApplicationType() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(null)
                    .coTenants(Collections.emptyList())
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldAcceptAloneWithNoCoTenants() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.ALONE)
                    .coTenants(Collections.emptyList())
                    .build();

            assertThat(validator.hasValidStructure(form)).isTrue();
        }

        @Test
        void shouldRejectAloneWithCoTenants() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.ALONE)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldRejectCoupleWhenMissingCoTenant() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .coTenants(Collections.emptyList())
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldRejectCoupleWithTwoCoTenants() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .coTenants(List.of(
                            new CoTenantForm("Louise", "Martin", null, "a@example.com"),
                            new CoTenantForm("Paul", "Martin", null, "b@example.com")
                    ))
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldRejectCoupleWithDuplicateEmails() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .coTenants(List.of(
                            new CoTenantForm("first", "one", null, "same@example.com"),
                            new CoTenantForm("second", "two", null, "same@example.com")
                    ))
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldAcceptGroupWithDistinctEmails() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .coTenants(List.of(
                            new CoTenantForm("first", "one", null, "a@example.com"),
                            new CoTenantForm("second", "two", null, "b@example.com")
                    ))
                    .build();

            assertThat(validator.hasValidStructure(form)).isTrue();
        }

        @Test
        void shouldRejectGroupWhenEmpty() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .coTenants(Collections.emptyList())
                    .build();

            assertThat(validator.hasValidStructure(form)).isFalse();
        }

        @Test
        void shouldIgnoreBlankEmailsWhenCheckingDistinctness() {
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .coTenants(List.of(
                            new CoTenantForm("first", "one", null, null),
                            new CoTenantForm("second", "two", null, "b@example.com")
                    ))
                    .build();

            assertThat(validator.hasValidStructure(form)).isTrue();
        }
    }

    @Nested
    class ValidateBusinessRules {

        @Test
        void shouldAcceptCoupleFormWithEmailAndConsent() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = coupleForm("spouse@example.com", true);

            assertThat(validator.validate(tenant, form)).isEmpty();
        }

        @Test
        void shouldRejectMissingEmailForCouple() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = coupleForm(null, true);

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_REQUIRED);
        }

        @Test
        void shouldRejectBlankEmailForCouple() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = coupleForm("   ", true);

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_REQUIRED);
        }

        @Test
        void shouldRejectMissingEmailForGroup() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(List.of(
                            new CoTenantForm("first", "one", null, "ok@example.com"),
                            new CoTenantForm("second", "two", null, null)
                    ))
                    .build();

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_REQUIRED);
        }

        @Test
        void shouldRejectMissingConsentForCouple() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = coupleForm("spouse@example.com", false);

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.ACCEPT_ACCESS_REQUIRED);
        }

        @Test
        void shouldRejectNullConsentForGroup() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(null)
                    .coTenants(List.of(new CoTenantForm("first", "one", null, "roommate@example.com")))
                    .build();

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.ACCEPT_ACCESS_REQUIRED);
        }

        @Test
        void shouldNotRequireConsentForAlone() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.ALONE)
                    .acceptAccess(null)
                    .coTenants(Collections.emptyList())
                    .build();

            assertThat(validator.validate(tenant, form)).isEmpty();
            verify(tenantRepository, never()).existsByEmail(anyString());
        }

        @Test
        void shouldRejectJoinTenantBeforeOtherRules() {
            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .tenantType(TenantType.JOIN)
                    .apartmentSharing(ApartmentSharing.builder().build())
                    .build();
            ApplicationFormV2 form = coupleForm(null, false);

            assertThat(validator.validate(tenant, form))
                    .contains(ApplicationErrorCode.APPLICATION_TYPE_DENIED_FOR_JOIN);
            verify(tenantRepository, never()).existsByEmail(anyString());
        }

        @Test
        void shouldRejectEmailAlreadyUsedInAnotherDossier() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = coupleForm("taken@example.com", true);

            when(tenantRepository.findByEmailInAndApartmentSharingNot(
                    eq(List.of("taken@example.com")), eq(tenant.getApartmentSharing())))
                    .thenReturn(List.of(Tenant.builder().email("taken@example.com").build()));

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
            verify(tenantRepository, never()).existsByEmail("taken@example.com");
        }

        @Test
        void shouldRejectEmailAlreadyRegisteredInSameDossier() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
            Tenant mainTenant = Tenant.builder()
                    .id(1L)
                    .email("principal@example.com")
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();
            apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant)));

            ApplicationFormV2 form = coupleForm("principal@example.com", true);

            when(tenantRepository.findByEmailInAndApartmentSharingNot(
                    eq(List.of("principal@example.com")), eq(apartmentSharing)))
                    .thenReturn(Collections.emptyList());
            when(tenantRepository.existsByEmail("principal@example.com")).thenReturn(true);

            assertThat(validator.validate(mainTenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
        }

        @Test
        void shouldAllowUnchangedCoTenantEmail() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
            Tenant coTenant = Tenant.builder()
                    .id(2L)
                    .firstName("Louise")
                    .lastName("Martin")
                    .email("spouse@example.com")
                    .build();
            Tenant mainTenant = Tenant.builder()
                    .id(1L)
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();
            apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant, coTenant)));

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                    .build();

            assertThat(validator.validate(mainTenant, form)).isEmpty();
            verify(tenantRepository, never()).existsByEmail(anyString());
        }

        @Test
        void shouldAllowUnchangedCoTenantEmailRegardlessOfCase() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
            Tenant coTenant = Tenant.builder()
                    .id(2L)
                    .firstName("Louise")
                    .lastName("Martin")
                    .email("Spouse@Example.COM")
                    .build();
            Tenant mainTenant = Tenant.builder()
                    .id(1L)
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();
            apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant, coTenant)));

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "spouse@example.com")))
                    .build();

            assertThat(validator.validate(mainTenant, form)).isEmpty();
        }

        @Test
        void shouldRejectNewEmailForExistingCoTenantWhenEmailAlreadyTaken() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder().build();
            Tenant coTenant = Tenant.builder()
                    .id(2L)
                    .firstName("Louise")
                    .lastName("Martin")
                    .build();
            Tenant mainTenant = Tenant.builder()
                    .id(1L)
                    .tenantType(TenantType.CREATE)
                    .apartmentSharing(apartmentSharing)
                    .build();
            apartmentSharing.setTenants(new ArrayList<>(List.of(mainTenant, coTenant)));

            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.COUPLE)
                    .acceptAccess(true)
                    .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, "NEW@example.com")))
                    .build();

            when(tenantRepository.findByEmailInAndApartmentSharingNot(
                    eq(List.of("new@example.com")), eq(apartmentSharing)))
                    .thenReturn(Collections.emptyList());
            when(tenantRepository.existsByEmail("new@example.com")).thenReturn(true);

            assertThat(validator.validate(mainTenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
        }

        @Test
        void shouldCheckEachGroupCoTenantEmailIndependently() {
            Tenant tenant = createMainTenant();
            ApplicationFormV2 form = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(List.of(
                            new CoTenantForm("first", "one", null, "free@example.com"),
                            new CoTenantForm("second", "two", null, "taken@example.com")
                    ))
                    .build();

            when(tenantRepository.findByEmailInAndApartmentSharingNot(
                    eq(List.of("free@example.com")), eq(tenant.getApartmentSharing())))
                    .thenReturn(Collections.emptyList());
            when(tenantRepository.findByEmailInAndApartmentSharingNot(
                    eq(List.of("taken@example.com")), eq(tenant.getApartmentSharing())))
                    .thenReturn(List.of(Tenant.builder().email("taken@example.com").build()));

            assertThat(validator.validate(tenant, form)).contains(ApplicationErrorCode.CO_TENANT_EMAIL_ALREADY_EXISTS);
        }
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

    private static ApplicationFormV2 coupleForm(String email, boolean acceptAccess) {
        return ApplicationFormV2.builder()
                .applicationType(ApplicationType.COUPLE)
                .acceptAccess(acceptAccess)
                .coTenants(List.of(new CoTenantForm("Louise", "Martin", null, email)))
                .build();
    }
}
