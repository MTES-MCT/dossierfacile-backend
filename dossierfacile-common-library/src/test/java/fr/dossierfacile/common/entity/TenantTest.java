package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.TenantOwnerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void should_return_user_preferred_name_when_owner_type_is_self() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.SELF).build();
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isEqualTo("Owner Preferred Name");
    }

    @Test
    void should_return_tenant_preferred_name_when_owner_type_is_third_party_and_tenant_preferred_name_is_set() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();
        tenant.setPreferredName("Third Party Preferred Name");
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isEqualTo("Third Party Preferred Name");
    }

    @Test
    void should_return_null_when_owner_type_is_third_party_and_tenant_preferred_name_is_missing() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();
        tenant.setUserPreferredName("Owner Preferred Name");

        assertThat(tenant.getPreferredName()).isNull();
    }

    @Test
    void should_return_null_when_owner_type_is_third_party_and_no_preferred_name_is_set() {
        Tenant tenant = Tenant.builder().ownerType(TenantOwnerType.THIRD_PARTY).build();

        assertThat(tenant.getPreferredName()).isNull();
    }
}

