package fr.dossierfacile.common.repository.projection;

import java.time.LocalDateTime;

public interface TenantWaitingTimeBucketProjection {
    Integer getBucketNo();
    Long getTenantCount();
    LocalDateTime getBucketMinLastUpdateDate();
    LocalDateTime getBucketMaxLastUpdateDate();
}
