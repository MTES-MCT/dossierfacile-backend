package fr.gouv.bo.dto;

import fr.dossierfacile.common.enums.TenantFileStatus;

import java.time.LocalDateTime;

public interface ApartmentSharingDTO01 {
    Integer getId();

    Integer getCountUserApis();

    String getPartnerId();

    LocalDateTime getCreationDate();

    String getFirstName();

    String getLastName();

    Integer getNumberOfTenants();

    Integer getNumberOfCompleteRegister();

    int getOrdinalStatus();

    default String getStatus() {
        return TenantFileStatus.values()[getOrdinalStatus()].name();
    }
}
