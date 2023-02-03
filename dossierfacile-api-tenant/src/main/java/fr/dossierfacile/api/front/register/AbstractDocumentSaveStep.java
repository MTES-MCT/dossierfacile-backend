package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.util.TransactionalUtil;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public abstract class AbstractDocumentSaveStep<T extends DocumentForm> implements SaveStep<T> {
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private Producer producer;
    @Autowired
    private DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, T documentForm) {
        Document document = saveDocument(tenant, documentForm);
        Long logId = documentPdfGenerationLogRepository.save(
                DocumentPdfGenerationLog.builder()
                        .documentId(document.getId())
                        .build()).getId();
        TransactionalUtil.afterCommit(() -> producer.generatePdf(document.getId(), logId));
        return tenantMapper.toTenantModel(document.getTenant());
    }

    protected abstract Document saveDocument(Tenant tenant, T documentForm);
}
