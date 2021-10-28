package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Message;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDeniedReasonsService {

    public final DocumentDeniedReasonsRepository documentDeniedReasonsRepository;

    @Transactional
    public void updateDocumentDeniedReasonsWithMessage(Message message, List<Long> ids) {
        documentDeniedReasonsRepository.updateDocumentDeniedReasonsWithMessage(message, ids);
    }
}
