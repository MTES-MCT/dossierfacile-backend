package fr.dossierfacile.common.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.model.log.DocumentLogDetails;
import fr.dossierfacile.common.model.log.FileLogDetails;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.repository.OperatorLogCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddLogDomainService {

    private final TenantLogRepository repository;
    private final ObjectMapper objectMapper;
    private final OperatorLogCommonRepository operatorLogRepository;
    private final TenantCommonRepository tenantCommonRepository;

    public void addDocumentDeletedLog(Document document, Tenant editor, Optional<Operator> operator) {
        DocumentLogDetails details = DocumentLogDetails.builder()
                .documentCategory(document.getDocumentCategory())
                .documentSubCategory(document.getDocumentSubCategory())
                .documentId(document.getId())
                .tenantId(document.getTenantId())
                .guarantorId(document.getGuarantorId())
                .build();

        var builder = TenantLog.builder()
                .logType(LogType.DOCUMENT_DELETED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now(ZoneId.systemDefault()))
                .logDetails(writeAsObjectNode(details));

        operator.ifPresent(o -> builder.operatorId(o.getId()));

        repository.save(builder.build());
    }

    public void addFileDeletedLog(FileEntity file, Tenant editor, Optional<Operator> operator) {
        var builder = TenantLog.builder()
                .logType(LogType.FILE_DELETED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now(ZoneId.systemDefault()))
                .logDetails(writeAsObjectNode(FileLogDetails.from(file)));

        operator.ifPresent(o -> builder.operatorId(o.getId()));

        repository.save(builder.build());
    }

    private ObjectNode writeAsObjectNode(Object object) {
        try {
            return objectMapper.valueToTree(object);
        } catch (IllegalArgumentException e) {
            log.error("FATAL: Cannot write log details as object node", e);
        }
        return null;
    }

    public void addAccountValidatedLog(Tenant tenant, Optional<User> operator) {
        TenantLog tenantLog = new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.map(User::getId).orElse(null));
        repository.save(tenantLog);

        operator.ifPresent(op -> {
            // TODO Quand OperatorLog va être transformer en entité "v2" il faudra viré le lien vers le Tenant en dur !
            // Comme ça on a pas besoin de load un tenant ici !
            fr.dossierfacile.common.entity.Tenant tenantProxy = tenantCommonRepository.getReferenceById(tenant.getId());
            operatorLogRepository.save(new OperatorLog(tenantProxy, op, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null));
        });
    }

    public void addAccountDeniedLog(Tenant tenant, Optional<User> operator) {
        TenantLog tenantLog = new TenantLog(LogType.ACCOUNT_DENIED, tenant.getId(), operator.map(User::getId).orElse(null), null);
        repository.save(tenantLog);

        operator.ifPresent(op -> {
            fr.dossierfacile.common.entity.Tenant tenantProxy = tenantCommonRepository.getReferenceById(tenant.getId());
            operatorLogRepository.save(new OperatorLog(tenantProxy, op, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null));
        });
    }
}
