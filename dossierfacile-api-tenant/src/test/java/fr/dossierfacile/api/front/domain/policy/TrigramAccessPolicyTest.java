package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Port of the legacy ApartmentSharingTrigramValidationTest onto the access policy
 * (same scenarios, Tenant aggregates instead of legacy entities).
 */
class TrigramAccessPolicyTest {

    private final TrigramAccessPolicy policy = new TrigramAccessPolicy();

    @Nested
    @DisplayName("Null, empty and blank trigram validation")
    class NullAndBlankTrigramTests {

        @Test
        @DisplayName("Should reject null, empty and blank trigram")
        void shouldRejectNullTrigram() {
            List<Tenant> tenants = List.of(selfTenant("Dupont", null));

            assertThatThrownBy(() -> policy.validateAccess(null, tenants))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("required");

            assertThatThrownBy(() -> policy.validateAccess("", tenants))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("required");

            assertThatThrownBy(() -> policy.validateAccess("   ", tenants))
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
            List<Tenant> tenants = List.of(selfTenant("Dupont", null));

            assertAccepted(tenants, "DUP");
        }

        @Test
        @DisplayName("Should accept trigram from lastName or preferredName")
        void shouldAcceptTrigramFromLastNameOrPreferredName() {
            List<Tenant> tenants = List.of(selfTenant("Dupont", "Martin"));

            assertAccepted(tenants, "MAR");
            assertAccepted(tenants, "DUP");
        }

        @Test
        @DisplayName("Should accept trigram case-insensitively")
        void shouldAcceptTrigramCaseInsensitively() {
            List<Tenant> tenants = List.of(selfTenant("Dupont", null));

            assertAccepted(tenants, "dup");
            assertAccepted(tenants, "Dup");
            assertAccepted(tenants, "DUP");
        }

        @Test
        @DisplayName("Should accept trigram with leading/trailing spaces")
        void shouldAcceptTrigramWithSpaces() {
            List<Tenant> tenants = List.of(selfTenant("Dupont", null));

            assertAccepted(tenants, "  DUP  ");
        }

        @Test
        @DisplayName("Should reject invalid trigram")
        void shouldRejectInvalidTrigram() {
            List<Tenant> tenants = List.of(selfTenant("Dupont", null));

            assertThatThrownBy(() -> policy.validateAccess("XXX", tenants))
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
            List<Tenant> tenants = List.of(thirdPartyTenant("Dupont", null, "Martin", null));

            assertAccepted(tenants, "MAR");
            assertAccepted(tenants, "DUP");
        }

        @Test
        @DisplayName("Should accept trigram from tenant preferredName or owner preferredName")
        void shouldAcceptTrigramFromTenantPreferredNameOrOwnerPreferredName() {
            List<Tenant> tenants = List.of(thirdPartyTenant("Dupont", "Henry", "Martin", "Dubois"));

            assertAccepted(tenants, "DUB");
            assertAccepted(tenants, "HEN");
            assertAccepted(tenants, "DUP");
            assertAccepted(tenants, "MAR");
        }

        @Test
        @DisplayName("Should handle THIRD_PARTY tenant with only tenantLastName set")
        void shouldHandleOnlyTenantLastNameSet() {
            List<Tenant> tenants = List.of(thirdPartyTenant(null, null, "Martin", null));

            assertAccepted(tenants, "MAR");
        }

        @Test
        @DisplayName("Should reject trigram not matching any of the four names")
        void shouldRejectInvalidTrigramForThirdParty() {
            List<Tenant> tenants = List.of(thirdPartyTenant("Dupont", "Henry", "Martin", "Dubois"));

            assertThatThrownBy(() -> policy.validateAccess("XXX", tenants))
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
            List<Tenant> tenants = List.of(selfTenant("Dupont", null), selfTenant("Martin", null));

            assertAccepted(tenants, "DUP");
            assertAccepted(tenants, "MAR");
        }

        @Test
        @DisplayName("Should accept trigram from any tenant in apartment sharing with third party tenants")
        void shouldAcceptTrigramFromAnyThirdPartyTenant() {
            List<Tenant> tenants = List.of(
                    thirdPartyTenant("Dupont", "Henry", "Martin", "Dubois"),
                    thirdPartyTenant("Quenneville", "Brousse", "Boivin", "Rochon"));

            assertAccepted(tenants, "DUP");
            assertAccepted(tenants, "HEN");
            assertAccepted(tenants, "MAR");
            assertAccepted(tenants, "DUB");

            assertAccepted(tenants, "Que");
            assertAccepted(tenants, "BRO");
            assertAccepted(tenants, "BOI");
            assertAccepted(tenants, "ROC");
        }

        @Test
        @DisplayName("Should reject any trigram when tenant list is empty")
        void shouldHandleEmptyTenantList() {
            assertThatThrownBy(() -> policy.validateAccess("DUP", Collections.emptyList()))
                    .isInstanceOf(TrigramNotAuthorizedException.class)
                    .hasMessageContaining("does not match");
        }
    }

    private void assertAccepted(List<Tenant> tenants, String trigram) {
        assertThatCode(() -> policy.validateAccess(trigram, tenants)).doesNotThrowAnyException();
    }

    private Tenant selfTenant(String lastName, String preferredName) {
        return new Tenant(TenantEntity.builder()
                .id(1L)
                .ownerType(TenantOwnerType.SELF)
                .lastName(lastName)
                .preferredName(preferredName)
                .build());
    }

    private Tenant thirdPartyTenant(String ownerLastName, String ownerPreferredName,
                                    String tenantLastName, String tenantPreferredName) {
        return new Tenant(TenantEntity.builder()
                .id(1L)
                .ownerType(TenantOwnerType.THIRD_PARTY)
                .lastName(ownerLastName)
                .preferredName(ownerPreferredName)
                .tenantLastName(tenantLastName)
                .tenantPreferredName(tenantPreferredName)
                .build());
    }
}
