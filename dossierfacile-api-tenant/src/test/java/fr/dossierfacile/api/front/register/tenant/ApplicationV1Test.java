package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApplicationV1Test {

    private final Tenant CO_TENANT_1 = Tenant.builder().id(1L).email("111@test.fr").build();
    private final Tenant CO_TENANT_2 = Tenant.builder().id(2L).email("222@test.fr").build();

    private final Application application = mock(Application.class);
    private final ApplicationV1 applicationV1 = new ApplicationV1(application);

    @Test
    void should_add_new_cotenant() {
        Tenant tenant = buildTenantWith(CO_TENANT_1);

        whenCoTenantsAreInvited(tenant, "111@test.fr", "222@test.fr");

        expectCreatedCoTenants(tenant, List.of("222@test.fr"));
        expectDeletedCoTenants(tenant, emptyList());
    }

    @Test
    void should_keep_existing_cotenant() {
        Tenant tenant = buildTenantWith(CO_TENANT_1);

        whenCoTenantsAreInvited(tenant, "111@test.fr");

        expectCreatedCoTenants(tenant, emptyList());
        expectDeletedCoTenants(tenant, emptyList());
    }

    @Test
    void should_remove_existing_cotenant() {
        Tenant tenant = buildTenantWith(CO_TENANT_1, CO_TENANT_2);

        whenCoTenantsAreInvited(tenant, "111@test.fr");

        expectCreatedCoTenants(tenant, emptyList());
        expectDeletedCoTenants(tenant, List.of(CO_TENANT_2));
    }

    @Test
    void should_remove_all_cotenant() {
        Tenant tenant = buildTenantWith(CO_TENANT_1, CO_TENANT_2);

        whenCoTenantsAreInvited(tenant);

        expectCreatedCoTenants(tenant, emptyList());
        expectDeletedCoTenants(tenant, List.of(CO_TENANT_1, CO_TENANT_2));
    }

    @Test
    void should_add_and_remove_cotenants() {
        Tenant tenant = buildTenantWith(CO_TENANT_1);

        whenCoTenantsAreInvited(tenant, "222@test.fr");

        expectDeletedCoTenants(tenant, List.of(CO_TENANT_1));
        expectCreatedCoTenants(tenant, List.of("222@test.fr"));
    }

    private static Tenant buildTenantWith(Tenant... existingCoTenants) {
        Tenant tenant = new Tenant();
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        tenant.setApartmentSharing(apartmentSharing);
        apartmentSharing.setTenants(List.of(existingCoTenants));
        return tenant;
    }

    private void whenCoTenantsAreInvited(Tenant tenant, String... invitedCoTenants) {
        ApplicationForm applicationForm = new ApplicationForm();
        applicationForm.setApplicationType(ApplicationType.GROUP);
        applicationForm.setCoTenantEmail(List.of(invitedCoTenants));
        applicationV1.saveStep(tenant, applicationForm);
    }

    private void expectCreatedCoTenants(Tenant tenant, List<String> createdCoTenantsEmails) {
        List<CoTenantForm> coTenantForms = createdCoTenantsEmails.stream()
                .map(email -> new CoTenantForm("", "", "", email))
                .collect(Collectors.toList());
        verify(application).saveStep(eq(tenant), eq(ApplicationType.GROUP), any(), eq(coTenantForms));
    }

    private void expectDeletedCoTenants(Tenant tenant, List<Tenant> deletedCoTenants) {
        verify(application).saveStep(eq(tenant), eq(ApplicationType.GROUP), eq(deletedCoTenants), any());
    }

}