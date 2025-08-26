package fr.dossierfacile.common.repository.projection;

import java.time.LocalDateTime;

public interface TenantLastStatusProjection {
    Long getTenantId();
    String getStatus();
    LocalDateTime getCreationDate();
}
