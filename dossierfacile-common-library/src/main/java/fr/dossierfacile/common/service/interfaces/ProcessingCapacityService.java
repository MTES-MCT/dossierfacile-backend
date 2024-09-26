package fr.dossierfacile.common.service.interfaces;

import java.time.LocalDateTime;

public interface ProcessingCapacityService {
    LocalDateTime getExpectedProcessingTime(Long tenantId);
}
