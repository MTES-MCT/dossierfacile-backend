package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantLastStatus {
    private Long tenantId;
    private LogType lastStatus;
    private LocalDateTime lastStatusDate;
    private Long lastLogId;
}


