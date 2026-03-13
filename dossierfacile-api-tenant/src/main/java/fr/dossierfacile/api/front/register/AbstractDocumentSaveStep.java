package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.model.ValidatedFile;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

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
    @Autowired
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @Autowired
    private FileUploadPreprocessor fileUploadPreprocessor;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, T documentForm) {
        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            tenant = tenantCommonRepository.save(tenant);
            logService.saveLog(LogType.ACCOUNT_RETURNED, tenant.getId());
            partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.RETURNED_ACCOUNT);
        }

        Document document = saveDocument(tenant, documentForm);
        logService.saveDocumentEditedLog(document, tenant, EditionType.ADD);
        documentService.markDocumentAsEdited(document);
        producer.sendDocumentForPdfGeneration(document);

        return tenantMapper.toTenantModel(document.getTenant() != null ? document.getTenant() : document.getGuarantor().getTenant(),
                (!clientAuthenticationFacade.isClient()) ? null : clientAuthenticationFacade.getClient());
    }

    protected abstract Document saveDocument(Tenant tenant, T documentForm);

    protected final void saveFiles(DocumentForm documentForm, Document document) {
        List<ValidatedFile> validatedFiles;
        try {
            validatedFiles = fileUploadPreprocessor.prepareValidatedFiles(documentForm.getDocuments());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire le fichier", e);
        }
        boolean allAllowed = validatedFiles.stream()
                .allMatch(v -> FileUploadPreprocessor.ALLOWED_MIME_TYPES.contains(v.detectedMimeType().toLowerCase()));
        if (!allAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid file type");
        }
        for (ValidatedFile vf : validatedFiles) {
            try {
                // TODO -> We must find a way to inform user there is a failure
                documentService.addFile(vf.file(), vf.detectedMimeType(), document);
            } catch (Exception e) {
                log.error("Unable to add File to document {}", document.getId(), e);
            }
        }
    }

}
