package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationV2Tests {

    private Application application = new Application(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );


    @Nested
    class GetTenantsToDelete {

        @Test
        void whenOldTenantsIsEmptyNoDelete() {
            List<Tenant> oldTenants = List.of();
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm(null, null, null, "test@test.fr"),
                                    new CoTenantForm(null, null, null, "test2@test.fr")
                            )
                    )
                    .build();
            var result = application.getTenantsToDelete(oldTenants,applicationForm);
            assertThat(result).isEmpty();
        }

        @Test
        void whenOldTenantsWithNamesNoDelete() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .firstName("test")
                            .lastName("test")
                            .email("test@test.fr")
                            .build(),
                    Tenant.builder()
                            .id(2L)
                            .firstName("test2")
                            .lastName("test2")
                            .email("test2@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm("test", "test", null, "test@test.fr"),
                                    new CoTenantForm("test2", "test2", null, "test2@test.fr")
                            )
                    )
                    .build();
            var result = application.getTenantsToDelete(oldTenants,applicationForm);
            assertThat(result).isEmpty();
        }

        @Test
        void whenOldTenantsWithoutNamesNoDelete() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .email("test@test.fr")
                            .build(),
                    Tenant.builder()
                            .id(2L)
                            .email("test2@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm(null, null, null, "test@test.fr"),
                                    new CoTenantForm(null, null, null, "test2@test.fr")
                            )
                    )
                    .build();
            var result = application.getTenantsToDelete(oldTenants,applicationForm);
            assertThat(result).isEmpty();
        }

        @Test
        void whenOldTenantsWithoutNamesDeleteOne() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .email("test@test.fr")
                            .build(),
                    Tenant.builder()
                            .id(2L)
                            .email("test2@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm(null, null, null, "test@test.fr")
                            )
                    )
                    .build();
            var result = application.getTenantsToDelete(oldTenants,applicationForm);
            assertThat(result).isNotEmpty().size().isEqualTo(1);
            assertThat(result.getFirst().getEmail()).isEqualTo("test2@test.fr");
        }

        @Test
        void whenOldTenantsWithNamesDeleteOne() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .firstName("test")
                            .lastName("test")
                            .email("test@test.fr")
                            .build(),
                    Tenant.builder()
                            .id(2L)
                            .firstName("test2")
                            .lastName("test2")
                            .email("test2@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm("test", "test", null, "test@test.fr")
                            )
                    )
                    .build();
            var result = application.getTenantsToDelete(oldTenants,applicationForm);
            assertThat(result).isNotEmpty().size().isEqualTo(1);
            assertThat(result.getFirst().getEmail()).isEqualTo("test2@test.fr");
        }


    }

    @Nested
    class GetTenantsToCreate {

        @Test
        void whenNoOldTenantsCreateOneWithoutNames() {
            List<Tenant> oldTenants = List.of();
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm(null, null, null, "test@test.fr")
                            )
                    )
                    .build();

            var result = application.getTenantsToCreate(oldTenants,applicationForm);
            assertThat(result).isNotEmpty().size().isEqualTo(1);
        }

        @Test
        void whenOldTenantsDoNotCreateWithoutNames() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .email("test@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm(null, null, null, "test@test.fr")
                            )
                    )
                    .build();

            var result = application.getTenantsToCreate(oldTenants,applicationForm);
            assertThat(result).isEmpty();
        }

        @Test
        void whenOldTenantsCreateOneWithNames() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .lastName("test")
                            .firstName("test")
                            .email("test@test.fr")
                            .build()
            );
            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm("test", "test", null, "test@test.fr"),
                                    new CoTenantForm(null, null, null, "test2@test.fr")
                            )
                    )
                    .build();

            var result = application.getTenantsToCreate(oldTenants,applicationForm);
            assertThat(result).isNotEmpty().size().isEqualTo(1);
            assertThat(result.getFirst().getEmail()).isEqualTo("test2@test.fr");
        }
    }

    @Nested
    class GetTenantWitNewEmailToUpdate {
        @Test
        void whenOldTenantsWithNamesUpdateOne() {
            List<Tenant> oldTenants = List.of(
                    Tenant.builder()
                            .id(1L)
                            .firstName("test")
                            .lastName("test")
                            .build()
            );

            var applicationForm = ApplicationFormV2.builder()
                    .applicationType(ApplicationType.GROUP)
                    .acceptAccess(true)
                    .coTenants(
                            List.of(
                                    new CoTenantForm("test", "test", null, "test@test2.fr")
                            )
                    )
                    .build();

            var result = application.getTenantWitNewEmailToUpdate(oldTenants, applicationForm);
            assertThat(result).isNotEmpty();

        }
    }
}
