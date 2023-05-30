package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;


class ApartmentSharingTest {

    @Test
    void should_return_archived() {
        Tenant tenant = Tenant.builder().status(TenantFileStatus.ARCHIVED).build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(Collections.singletonList(tenant)).build();

        Assertions.assertEquals(TenantFileStatus.ARCHIVED, as.getStatus());
    }

    @Test
    void should_return_archived_with_several_tenants() {
        Tenant tenant = Tenant.builder().status(TenantFileStatus.ARCHIVED).build();
        Tenant tenant2 = Tenant.builder().status(TenantFileStatus.ARCHIVED).build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(Arrays.asList(tenant, tenant2)).build();

        Assertions.assertEquals(TenantFileStatus.ARCHIVED, as.getStatus());
    }

    @Test
    void should_return_incomplete_with_several_tenants() {
        Tenant tenant = Tenant.builder().status(TenantFileStatus.ARCHIVED).build();
        Tenant tenant2 = Tenant.builder().status(TenantFileStatus.TO_PROCESS).build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(Arrays.asList(tenant, tenant2)).build();

        Assertions.assertEquals(TenantFileStatus.INCOMPLETE, as.getStatus());
    }

    @Test
    void should_return_declined_with_several_tenants() {
        Tenant tenant = Tenant.builder().status(TenantFileStatus.ARCHIVED).build();
        Tenant tenant2 = Tenant.builder().status(TenantFileStatus.TO_PROCESS).build();
        Tenant tenant3 = Tenant.builder().status(TenantFileStatus.DECLINED).build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(Arrays.asList(tenant, tenant2, tenant3)).build();

        Assertions.assertEquals(TenantFileStatus.DECLINED, as.getStatus());
    }

    @Test
    void should_return_validated_with_several_tenants() {
        Tenant tenant = Tenant.builder().status(TenantFileStatus.VALIDATED).build();
        Tenant tenant2 = Tenant.builder().status(TenantFileStatus.VALIDATED).build();
        Tenant tenant3 = Tenant.builder().status(TenantFileStatus.VALIDATED).build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(Arrays.asList(tenant, tenant2, tenant3)).build();

        Assertions.assertEquals(TenantFileStatus.VALIDATED, as.getStatus());
    }
}