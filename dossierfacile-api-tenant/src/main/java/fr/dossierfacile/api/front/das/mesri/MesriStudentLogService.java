package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

/** Tenant logs of the POC, used to measure it. Failures are logged in their own transaction so a rollback keeps them. */
@Service
@RequiredArgsConstructor
public class MesriStudentLogService {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    public void logSuccess(Long tenantId, MesriStudentStatus status, FranceConnectAccessToken token) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("admissionCount", status.admissions().size());
        details.put("fcScope", token.scope());
        status.admissions().stream()
                .max(Comparator.comparing(a -> a.dateFin() == null ? "" : a.dateFin()))
                .ifPresent(latest -> {
                    details.put("latestDateFin", latest.dateFin());
                    details.put("latestEstInscrit", latest.estInscrit());
                    details.put("latestUai", latest.uai());
                    details.put("latestRegime", latest.regimeCode());
                });
        logService.saveLogWithDetails(LogType.DAS_MESRI_STUDENT_FETCHED, tenantId, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(Long tenantId, RuntimeException cause) {
        ObjectNode details = objectMapper.createObjectNode();
        details.put("reason", cause.getClass().getSimpleName());
        details.put("message", cause.getMessage());
        logService.saveLogWithDetails(LogType.DAS_MESRI_STUDENT_FAILED, tenantId, details);
    }
}
