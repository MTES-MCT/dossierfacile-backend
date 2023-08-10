package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDeniedReasonsService {

    public final DocumentDeniedReasonsRepository documentDeniedReasonsRepository;
    private final LogService logService;

    @Transactional
    public void updateDocumentDeniedReasonsWithMessage(Message message, List<Long> ids) {
        documentDeniedReasonsRepository.updateDocumentDeniedReasonsWithMessage(message, ids);
    }

    public Optional<DocumentDeniedReasons> getLastDeniedReason(Document document, Tenant tenant) {
        Optional<LocalDateTime> lastValidation = logService.getLogByTenantId(tenant.getId())
                .stream()
                .filter(log -> log.getLogType() == LogType.ACCOUNT_VALIDATED)
                .map(Log::getCreationDateTime)
                .max(Comparator.naturalOrder());

        return documentDeniedReasonsRepository.findByDocumentId(document.getId())
                .stream()
                .sorted(Comparator.comparing(DocumentDeniedReasons::getCreationDate).reversed())
                .filter(reason -> lastValidation.map(date -> reason.getCreationDate().isAfter(date)).orElse(true))
                .findFirst();
    }

}
