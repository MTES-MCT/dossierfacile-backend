package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.util.TransactionalUtil;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractDocumentSaveStep<T extends DocumentForm> implements SaveStep<T> {
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private Producer producer;
    @Autowired
    private DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;
    @Autowired
    private DocumentService documentService;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, T documentForm) {
        Document document = saveDocument(tenant, documentForm);
        Long logId = documentPdfGenerationLogRepository.save(
                DocumentPdfGenerationLog.builder()
                        .documentId(document.getId())
                        .build()).getId();
        TransactionalUtil.afterCommit(() -> producer.generatePdf(document.getId(), logId));
        return tenantMapper.toTenantModel(document.getTenant() != null ? document.getTenant() : document.getGuarantor().getTenant());
    }

    protected abstract Document saveDocument(Tenant tenant, T documentForm);

    protected final void saveFiles(DocumentForm documentForm, Document document) {
        documentForm.getDocuments().stream()
                .filter(file -> !file.isEmpty())
                .forEach(file -> {
                    try {
                        // TODO -> We must find a way to inform user there is a failure
                        documentService.addFile(file, document);
                    } catch (IOException ioe) {
                        log.error("Unable to add File ", ioe);
                    }
                });
    }

}
