package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.gouv.bo.service.KeycloakService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOE2ETestControllerTest {

    private static final String ALLOWED_EMAIL_PATTERN =
            "(ywiwyne-1268@yopmail\\.com|[a-z0-9]+\\.[a-z0-9_-]+@inbox\\.testmail\\.app)";
    private static final String OPERATOR_EMAIL = "e2e-tests@dossierfacile.fr";
    private static final String TEST_EMAIL = "namespace1.e2e-alone@inbox.testmail.app";
    private static final Long TENANT_ID = 42L;

    @Mock
    private TenantService tenantService;
    @Mock
    private UserService userService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private Environment environment;

    @InjectMocks
    private BOE2ETestController controller;

    private final BOUser operator = BOUser.builder().email(OPERATOR_EMAIL).build();
    private final Tenant tenant = Tenant.builder().id(TENANT_ID).email(TEST_EMAIL).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "allowedEmailPattern", ALLOWED_EMAIL_PATTERN);
        ReflectionTestUtils.setField(controller, "operatorEmail", OPERATOR_EMAIL);
        controller.compileAllowedEmailPattern();
    }

    private void givenProfile(String profile) {
        when(environment.getActiveProfiles()).thenReturn(new String[]{profile});
    }

    @Nested
    class ProductionGuard {

        @Test
        void allEndpointsReturn403InProduction() {
            givenProfile("prod");

            assertThat(controller.validateTenant(TEST_EMAIL).getStatusCode().value()).isEqualTo(403);
            assertThat(controller.declineTenant(TEST_EMAIL, declineRequest(null)).getStatusCode().value()).isEqualTo(403);
            assertThat(controller.verifyEmail(TEST_EMAIL).getStatusCode().value()).isEqualTo(403);
            assertThat(controller.deleteUser(TEST_EMAIL).getStatusCode().value()).isEqualTo(403);

            verifyNoInteractions(tenantService, userService, keycloakService);
        }
    }

    @Nested
    class EmailAllowlist {

        @Test
        void rejectsEmailOutsidePattern() {
            givenProfile("dev");

            assertThat(controller.deleteUser("someone@gmail.com").getStatusCode().value()).isEqualTo(400);

            verifyNoInteractions(tenantService, userService, keycloakService);
        }

        @Test
        void rejectsTestmailLookalikeDomain() {
            givenProfile("dev");

            ResponseEntity<Void> response =
                    controller.deleteUser("namespace1.tag@inbox.testmail.app.evil.com");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(tenantService, userService, keycloakService);
        }

        @Test
        void rejectsPrefixedNamespace() {
            givenProfile("dev");

            ResponseEntity<Void> response =
                    controller.deleteUser("evil-namespace1.tag@inbox.testmail.app");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(tenantService, userService, keycloakService);
        }

        @Test
        void matchingIsCaseInsensitive() {
            givenProfile("dev");
            String uppercaseEmail = "NAMESPACE1.E2E-ALONE@INBOX.TESTMAIL.APP";
            when(keycloakService.markEmailAsVerified(uppercaseEmail)).thenReturn(true);

            assertThat(controller.verifyEmail(uppercaseEmail).getStatusCode().value()).isEqualTo(200);
        }

        @Test
        void acceptsLegacyDevEmail() {
            givenProfile("dev");
            String legacyEmail = "ywiwyne-1268@yopmail.com";
            when(keycloakService.markEmailAsVerified(legacyEmail)).thenReturn(true);

            assertThat(controller.verifyEmail(legacyEmail).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    class VerifyEmail {

        @Test
        void returns200WhenKeycloakAccountIsVerified() {
            givenProfile("dev");
            when(keycloakService.markEmailAsVerified(TEST_EMAIL)).thenReturn(true);

            assertThat(controller.verifyEmail(TEST_EMAIL).getStatusCode().value()).isEqualTo(200);
        }

        @Test
        void returns404WhenKeycloakAccountDoesNotExist() {
            givenProfile("dev");
            when(keycloakService.markEmailAsVerified(TEST_EMAIL)).thenReturn(false);

            assertThat(controller.verifyEmail(TEST_EMAIL).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void returns500OnKeycloakError() {
            givenProfile("dev");
            when(keycloakService.markEmailAsVerified(TEST_EMAIL)).thenThrow(new RuntimeException("keycloak down"));

            assertThat(controller.verifyEmail(TEST_EMAIL).getStatusCode().value()).isEqualTo(500);
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void deletesTenantWithE2EOperatorWhenTenantExists() {
            givenProfile("dev");
            when(tenantService.findTenantByEmailOptional(TEST_EMAIL)).thenReturn(Optional.of(tenant));
            when(userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL)).thenReturn(operator);
            when(keycloakService.deleteKeycloakUserByEmail(TEST_EMAIL)).thenReturn(false);

            assertThat(controller.deleteUser(TEST_EMAIL).getStatusCode().value()).isEqualTo(200);

            verify(userService).deleteApartmentSharing(tenant, operator);
        }

        @Test
        void deletesOrphanKeycloakAccountWhenNoTenantInDatabase() {
            givenProfile("dev");
            when(tenantService.findTenantByEmailOptional(TEST_EMAIL)).thenReturn(Optional.empty());
            when(keycloakService.deleteKeycloakUserByEmail(TEST_EMAIL)).thenReturn(true);

            assertThat(controller.deleteUser(TEST_EMAIL).getStatusCode().value()).isEqualTo(200);

            verify(userService, never()).deleteApartmentSharing(any(Tenant.class), any(BOUser.class));
        }

        @Test
        void returns404WhenNothingToDelete() {
            givenProfile("dev");
            when(tenantService.findTenantByEmailOptional(TEST_EMAIL)).thenReturn(Optional.empty());
            when(keycloakService.deleteKeycloakUserByEmail(TEST_EMAIL)).thenReturn(false);

            assertThat(controller.deleteUser(TEST_EMAIL).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void returns500WhenDeletionFails() {
            givenProfile("dev");
            when(tenantService.findTenantByEmailOptional(TEST_EMAIL)).thenReturn(Optional.of(tenant));
            when(userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL)).thenReturn(operator);
            doThrow(new RuntimeException("boom"))
                    .when(userService).deleteApartmentSharing(tenant, operator);

            assertThat(controller.deleteUser(TEST_EMAIL).getStatusCode().value()).isEqualTo(500);
        }
    }

    @Nested
    class ValidateTenant {

        @Test
        void validatesTenantWithE2EOperator() {
            givenProfile("dev");
            when(tenantService.findTenantByEmail(TEST_EMAIL)).thenReturn(tenant);
            when(userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL)).thenReturn(operator);

            assertThat(controller.validateTenant(TEST_EMAIL).getStatusCode().value()).isEqualTo(200);

            verify(tenantService).validateTenantFile(TENANT_ID, operator);
        }

        @Test
        void returns404WhenTenantDoesNotExist() {
            givenProfile("dev");
            when(tenantService.findTenantByEmail(TEST_EMAIL)).thenThrow(new NotFoundException("not found"));

            assertThat(controller.validateTenant(TEST_EMAIL).getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    class DeclineTenant {

        @Test
        void declinesTenantWithE2EOperatorAndMappedCategories() {
            givenProfile("dev");
            when(tenantService.findTenantByEmail(TEST_EMAIL)).thenReturn(tenant);
            when(userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL)).thenReturn(operator);

            ResponseEntity<Void> response =
                    controller.declineTenant(TEST_EMAIL, declineRequest(List.of("IDENTIFICATION")));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(tenantService).declineTenantForTesting(
                    TENANT_ID, operator, "message", List.of(DocumentCategory.IDENTIFICATION));
        }

        @Test
        void nullCategoriesMeansDeclineAllDocuments() {
            givenProfile("dev");
            when(tenantService.findTenantByEmail(TEST_EMAIL)).thenReturn(tenant);
            when(userService.findOrCreateOperatorByEmail(OPERATOR_EMAIL)).thenReturn(operator);

            ResponseEntity<Void> response = controller.declineTenant(TEST_EMAIL, declineRequest(null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(tenantService).declineTenantForTesting(
                    TENANT_ID, operator, "message", Collections.emptyList());
        }

        @Test
        void returns500OnUnknownCategory() {
            givenProfile("dev");
            when(tenantService.findTenantByEmail(TEST_EMAIL)).thenReturn(tenant);

            ResponseEntity<Void> response =
                    controller.declineTenant(TEST_EMAIL, declineRequest(List.of("NOT_A_CATEGORY")));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }
    }

    private static BOE2ETestController.DeclineRequest declineRequest(List<String> categories) {
        return new BOE2ETestController.DeclineRequest("message", categories);
    }
}
