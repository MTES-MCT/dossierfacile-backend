package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.exceptions.ConfirmationTokenNotFoundException;
import fr.dossierfacile.common.repository.ConfirmationTokenRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.garbagecollector.service.interfaces.DocumentService;
import fr.dossierfacile.garbagecollector.service.interfaces.GuarantorService;
import fr.dossierfacile.garbagecollector.service.interfaces.MailService;
import fr.dossierfacile.garbagecollector.service.interfaces.TenantWarningService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TenantWarningServiceImpl implements TenantWarningService {
    private final LogService logService;
    private final MailService mailService;
    private final ConfirmationTokenService confirmationTokenService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final DocumentService documentService;
    private final GuarantorService guarantorService;
    private final TenantCommonRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;

    @Transactional
    @Override
    public void handleTenantWarning(Tenant t, int warnings) {
        switch (warnings) {
            case 0 -> handleWarning0(t);
            case 1 -> handleWarning1(t);
            case 2 -> handleWarning2(t);
        }
        tenantRepository.save(t);
    }

    private void handleWarning2(Tenant t) {
        log.info("accountWarnings. Documents deletion for tenant with ID [" + t.getId() + "]");
        t.setWarnings(0);
        t.setConfirmationToken(null);
        t.setHonorDeclaration(false);
        t.setStatus(TenantFileStatus.ARCHIVED);
        t.setZipCode("");
        t.setClarification("");

        ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(t).orElseThrow(() -> new ConfirmationTokenNotFoundException(t.getId()));

        documentService.deleteAllDocumentsAssociatedToTenant(t);
        guarantorService.deleteAllGuaratorsAssociatedToTenant(t);
        logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, t.getId());
        partnerCallBackService.sendCallBack(t, PartnerCallBackType.ARCHIVED_ACCOUNT);
        confirmationTokenRepository.delete(confirmationToken);
    }

    private void handleWarning1(Tenant t) {
        log.info("accountWarnings. SECOND warning for tenant with ID [" + t.getId() + "]");
        t.setWarnings(2);
        mailService.sendEmailSecondWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
        logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
    }

    private void handleWarning0(Tenant t) {
        log.info("accountWarnings. FIRST warning for tenant with ID [" + t.getId() + "]");
        t.setWarnings(1);
        mailService.sendEmailFirstWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
        logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
    }
}
