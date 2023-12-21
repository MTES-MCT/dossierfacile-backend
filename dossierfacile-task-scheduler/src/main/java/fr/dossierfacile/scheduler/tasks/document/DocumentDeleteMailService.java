package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sendinblue.ApiException;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDeleteMailService {
    private final TransactionalEmailsApi apiInstance;
    private final TenantCommonRepository tenantCommonRepository;
    @Value("${sendinblue.template.id.deleted.document.with.failed.pdf}")
    private Long templateDeletedDocumentWithFailedPdf;

    @Transactional(readOnly = true)
    public void sendMailWithDocumentFailed(Long tenantId, List<Document> documents) {
        log.debug("Send a email to {} with {} documents", tenantId, documents.size());
        Tenant tenant = tenantCommonRepository.findById(tenantId).get();
        if (isNotBlank(tenant.getEmail())) {
            Map<String, String> variables = new HashMap<>();
            variables.put("PRENOM", tenant.getFirstName());

            SendSmtpEmailTo sendSmtpEmailTo = new SendSmtpEmailTo();
            sendSmtpEmailTo.setEmail(tenant.getEmail());

            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.templateId(templateDeletedDocumentWithFailedPdf);
            sendSmtpEmail.params(variables);
            sendSmtpEmail.to(Collections.singletonList(sendSmtpEmailTo));

            try {
                apiInstance.sendTransacEmail(sendSmtpEmail);
            } catch (ApiException e) {
                log.error("Email api exception", e);
            }
        }

    }
}
