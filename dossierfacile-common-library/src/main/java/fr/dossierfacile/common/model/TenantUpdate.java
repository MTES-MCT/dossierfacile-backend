package fr.dossierfacile.common.model;

import java.time.LocalDateTime;

public interface TenantUpdate {
    Long getId();

    LocalDateTime getLastUpdateDate();
}