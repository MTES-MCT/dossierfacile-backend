package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantOwnerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApartmentSharingTrigramValidationTest {

    private ApartmentSharingServiceImpl service;
    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void setUp() {
        // Create service with null dependencies (not needed for validateAndNormalizeTrigram)
        service = new ApartmentSharingServiceImpl(
                null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .build();
    }

    @Nested
    @DisplayName("Null, empty and blank trigram validation")
    class NullAndBlankTrigramTests {

        @Test
        @DisplayName("Should reject null, empty and blank trigram")
        void shouldRejectNullTrigram() {
            Tenant tenant = createSelfTenant("Dupont", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, null))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("required");

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, ""))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("required");

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, "   "))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("SELF tenant trigram validation")
    class SelfTenantTests {

        @Test
        @DisplayName("Should accept trigram from lastName")
        void shouldAcceptTrigramFromLastName() {
            Tenant tenant = createSelfTenant("Dupont", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram from lastName or preferredName")
        void shouldAcceptTrigramFromLastNameOrPreferredName() {
            Tenant tenant = createSelfTenant("Dupont", "Martin");
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram case-insensitively")
        void shouldAcceptTrigramCaseInsensitively() {
            Tenant tenant = createSelfTenant("Dupont", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "dup")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "Dup")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram with leading/trailing spaces")
        void shouldAcceptTrigramWithSpaces() {
            Tenant tenant = createSelfTenant("Dupont", null);
            apartmentSharing.setTenants(List.of(tenant));

            boolean result = service.validateAndNormalizeTrigram(apartmentSharing, "  DUP  ");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid trigram")
        void shouldRejectInvalidTrigram() {
            Tenant tenant = createSelfTenant("Dupont", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, "XXX"))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("does not match");
        }
    }

    @Nested
    @DisplayName("THIRD_PARTY tenant trigram validation")
    class ThirdPartyTenantTests {

        @Test
        @DisplayName("Should accept trigram from tenant lastName or owner lastName")
        void shouldAcceptTrigramFromTenantLastNameOrOwnerLastName() {
            Tenant tenant = createThirdPartyTenant("Dupont", null, "Martin", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram from tenant preferredName or owner preferredName")
        void shouldAcceptTrigramFromTenantPreferredNameOrOwnerPreferredName() {
            Tenant tenant = createThirdPartyTenant("Dupont", "Henry", "Martin", "Dubois");
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUB")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "HEN")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram from tenant preferredName")
        void shouldAcceptTrigramFromTenantPreferredName() {
            Tenant tenant = createThirdPartyTenant("Dupont", null, "Martin", "Dubois");
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUB")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
        }

        @Test
        @DisplayName("Should handle THIRD_PARTY tenant with only tenantLastName set")
        void shouldHandleOnlyTenantLastNameSet() {
            Tenant tenant = createThirdPartyTenant(null, null, "Martin", null);
            apartmentSharing.setTenants(List.of(tenant));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
        }

        @Test
        @DisplayName("Should reject trigram not matching any of the four names")
        void shouldRejectInvalidTrigramForThirdParty() {
            Tenant tenant = createThirdPartyTenant("Dupont", "Henry", "Martin", "Dubois");
            apartmentSharing.setTenants(List.of(tenant));

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, "XXX"))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("does not match");
        }
    }

    @Nested
    @DisplayName("Multiple tenants validation")
    class MultipleTenantTests {

        @Test
        @DisplayName("Should accept trigram from any tenant in apartment sharing with self tenants")
        void shouldAcceptTrigramFromAnyTenant() {
            Tenant tenant1 = createSelfTenant("Dupont", null);
            Tenant tenant2 = createSelfTenant("Martin", null);
            apartmentSharing.setTenants(List.of(tenant1, tenant2));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
        }

        @Test
        @DisplayName("Should accept trigram from any tenant in apartment sharing with third party tenants")
        void shouldAcceptTrigramFromAnyThirdPartyTenant() {
            Tenant tenant1 = createThirdPartyTenant("Dupont", "Henry", "Martin", "Dubois");
            Tenant tenant2 = createThirdPartyTenant("Quenneville", "Brousse", "Boivin", "Rochon");
            apartmentSharing.setTenants(List.of(tenant1, tenant2));

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUP")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "HEN")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "MAR")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "DUB")).isTrue();

            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "Que")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "BRO")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "BOI")).isTrue();
            assertThat(service.validateAndNormalizeTrigram(apartmentSharing, "ROC")).isTrue();
        }

        @Test
        @DisplayName("Should handle empty tenant list")
        void shouldHandleEmptyTenantList() {
            apartmentSharing.setTenants(Collections.emptyList());

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, "DUP"))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("does not match");
        }

        @Test
        @DisplayName("Should handle null tenant list")
        void shouldHandleNullTenantList() {
            apartmentSharing.setTenants(null);

            assertThatThrownBy(() -> service.validateAndNormalizeTrigram(apartmentSharing, "DUP"))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("does not match");
        }
    }

    // Helper methods to create tenants

    private Tenant createSelfTenant(String lastName, String preferredName) {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .ownerType(TenantOwnerType.SELF)
                .apartmentSharing(apartmentSharing)
                .build();
        tenant.setLastName(lastName);
        tenant.setPreferredName(preferredName);
        return tenant;
    }

    private Tenant createThirdPartyTenant(String ownerLastName, String ownerPreferredName,
                                           String tenantLastName, String tenantPreferredName) {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .ownerType(TenantOwnerType.THIRD_PARTY)
                .apartmentSharing(apartmentSharing)
                .build();
        tenant.setUserLastName(ownerLastName);
        tenant.setUserPreferredName(ownerPreferredName);
        tenant.setLastName(tenantLastName);
        tenant.setPreferredName(tenantPreferredName);
        return tenant;
    }
}

