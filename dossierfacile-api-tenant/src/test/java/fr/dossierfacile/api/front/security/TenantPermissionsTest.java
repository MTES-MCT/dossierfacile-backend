package fr.dossierfacile.api.front.security;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class TenantPermissionsTest {

    @Nested
    class AloneApplicationType implements PermissionTest {

        @Override
        public Tenant tenantToTest() {
            return buildTenant(1L, ApplicationType.ALONE);
        }

        @Override
        public List<Long> tenantsThatShouldBeAccessible() {
            return List.of(1L);
        }

        @Override
        public List<Long> tenantsThatShouldNotBeAccessible() {
            return List.of(99L);
        }
    }

    @Nested
    class GroupApplicationType implements PermissionTest {

        @Override
        public Tenant tenantToTest() {
            return buildTenantWithRoommate(1L, ApplicationType.GROUP, 2L);
        }

        @Override
        public List<Long> tenantsThatShouldBeAccessible() {
            return List.of(1L);
        }

        @Override
        public List<Long> tenantsThatShouldNotBeAccessible() {
            return List.of(2L, 99L);
        }
    }

    @Nested
    class CoupleApplicationType implements PermissionTest {

        @Override
        public Tenant tenantToTest() {
            return buildTenantWithRoommate(1L, ApplicationType.COUPLE, 2L);
        }

        @Override
        public List<Long> tenantsThatShouldBeAccessible() {
            return List.of(1L, 2L);
        }

        @Override
        public List<Long> tenantsThatShouldNotBeAccessible() {
            return List.of(99L);
        }

    }

    interface PermissionTest {

        Tenant tenantToTest();

        List<Long> tenantsThatShouldBeAccessible();

        List<Long> tenantsThatShouldNotBeAccessible();

        @Test
        default void should_return_true_for_null_input() {
            assertTrue(hasPermissionOn(null));
        }

        @TestFactory
        default Stream<DynamicTest> accessibleTenants() {
            return tenantsThatShouldBeAccessible().stream()
                    .map(tenantId -> dynamicTest("should have access to tenant " + tenantId,
                            () -> assertTrue(hasPermissionOn(tenantId))));
        }

        @TestFactory
        default Stream<DynamicTest> nonAccessibleTenants() {
            return tenantsThatShouldNotBeAccessible().stream()
                    .map(tenantId -> dynamicTest("should not have access to tenant " + tenantId,
                            () -> assertFalse(hasPermissionOn(tenantId))));
        }

        private boolean hasPermissionOn(Long tenantId) {
            return new TenantPermissions(tenantToTest()).canAccess(tenantId);
        }


    }

    private static Tenant buildTenant(long id, ApplicationType applicationType) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setApplicationType(applicationType);
        tenant.setApartmentSharing(apartmentSharing);
        return tenant;
    }

    private Tenant buildTenantWithRoommate(long tenantId, ApplicationType applicationType, long roommateId) {
        Tenant tenant = buildTenant(tenantId, applicationType);
        Tenant roommate = new Tenant();
        roommate.setId(roommateId);
        tenant.getApartmentSharing().setTenants(List.of(roommate));
        return tenant;
    }

}