package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.util.TransactionalUtil;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
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
    private DocumentService documentService;
    @Autowired
    private PartnerCallBackService partnerCallBackService;
    @Autowired
    private TenantCommonRepository tenantCommonRepository;
    @Autowired
    private LogService logService;
    @Autowired
    private Producer producer;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, T documentForm) {
        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            tenant = tenantCommonRepository.save(tenant);
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.RETURNED_ACCOUNT);
        }

        Document document = saveDocument(tenant, documentForm);
        logService.saveDocumentEditedLog(document, tenant, EditionType.ADD);
        documentService.markDocumentAsEdited(document);

        TransactionalUtil.afterCommit(() -> {
            producer.sendDocumentForAnalysis(document);
            producer.sendDocumentForPdfGeneration(document);
        });

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
