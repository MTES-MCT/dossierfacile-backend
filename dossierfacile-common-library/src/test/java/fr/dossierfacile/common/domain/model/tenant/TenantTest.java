package fr.dossierfacile.common.domain.model.tenant;

import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void should_update_last_update_date() {
        TenantEntity entity = TenantEntity.builder().build();
        Tenant tenant = new Tenant(entity);

        assertThat(entity.getLastUpdateDate()).isNull();

        tenant.updateLastUpdateDate();

        assertThat(entity.getLastUpdateDate()).isNotNull();
        assertThat(entity.getLastUpdateDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    /**
     * The identity accessors must replicate the overridden getters of the legacy entity
     * (common/entity/Tenant.java): SELF reads the user_account columns, THIRD_PARTY the
     * tenant_* columns, an untyped tenant falls back from tenant_* to user_account.
     */
    @Nested
    @DisplayName("Identity accessors (legacy getters parity)")
    class IdentityAccessors {

        private Tenant tenant(TenantOwnerType ownerType, String tenantFirstName, String tenantLastName, String tenantPreferredName) {
            return new Tenant(TenantEntity.builder()
                    .ownerType(ownerType)
                    .firstName("UserFirst")
                    .lastName("UserLast")
                    .preferredName("UserPreferred")
                    .tenantFirstName(tenantFirstName)
                    .tenantLastName(tenantLastName)
                    .tenantPreferredName(tenantPreferredName)
                    .build());
        }

        @Test
        void self_reads_user_account_columns_even_when_tenant_columns_are_set() {
            Tenant self = tenant(TenantOwnerType.SELF, "TenantFirst", "TenantLast", "TenantPreferred");

            assertThat(self.getFirstName()).isEqualTo("UserFirst");
            assertThat(self.getLastName()).isEqualTo("UserLast");
            assertThat(self.getPreferredName()).isEqualTo("UserPreferred");
        }

        @Test
        void third_party_reads_tenant_columns() {
            Tenant thirdParty = tenant(TenantOwnerType.THIRD_PARTY, "TenantFirst", "TenantLast", "TenantPreferred");

            assertThat(thirdParty.getFirstName()).isEqualTo("TenantFirst");
            assertThat(thirdParty.getLastName()).isEqualTo("TenantLast");
            assertThat(thirdParty.getPreferredName()).isEqualTo("TenantPreferred");
        }

        @Test
        void third_party_preferred_name_has_no_fallback() {
            Tenant thirdParty = tenant(TenantOwnerType.THIRD_PARTY, "TenantFirst", "TenantLast", null);

            assertThat(thirdParty.getPreferredName()).isNull();
        }

        @Test
        void third_party_names_fall_back_to_user_account_when_tenant_columns_are_null() {
            Tenant thirdParty = tenant(TenantOwnerType.THIRD_PARTY, null, null, null);

            assertThat(thirdParty.getFirstName()).isEqualTo("UserFirst");
            assertThat(thirdParty.getLastName()).isEqualTo("UserLast");
        }

        @Test
        void null_owner_type_falls_back_from_tenant_columns_to_user_account() {
            Tenant untypedWithTenantNames = tenant(null, "TenantFirst", "TenantLast", null);
            assertThat(untypedWithTenantNames.getFirstName()).isEqualTo("TenantFirst");
            assertThat(untypedWithTenantNames.getLastName()).isEqualTo("TenantLast");
            assertThat(untypedWithTenantNames.getPreferredName()).isEqualTo("UserPreferred");

            Tenant untypedWithoutTenantNames = tenant(null, null, null, null);
            assertThat(untypedWithoutTenantNames.getFirstName()).isEqualTo("UserFirst");
            assertThat(untypedWithoutTenantNames.getLastName()).isEqualTo("UserLast");
        }

        @Test
        void user_columns_are_exposed_raw_for_the_trigram_policy() {
            Tenant thirdParty = tenant(TenantOwnerType.THIRD_PARTY, "TenantFirst", "TenantLast", "TenantPreferred");

            assertThat(thirdParty.getUserLastName()).isEqualTo("UserLast");
            assertThat(thirdParty.getUserPreferredName()).isEqualTo("UserPreferred");
        }
    }
}
