package fr.gouv.owner.projection;

import fr.dossierfacile.common.enums.TenantFileStatus;

public interface ApartmentSharingDTO02 {

    Integer getId();

    int getNumberOfTenants();

    int getNumberOfCompleteRegister();

    int getStatus();

    int getTotalSalaryTenant();

    String getToken();

    String getTokenPublic();

    default TenantFileStatus getFileStatus() {
        return TenantFileStatus.values()[getStatus()];
    }
}
