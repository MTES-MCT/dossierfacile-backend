package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.repository.GuarantorRepository;
import fr.dossierfacile.api.pdfgenerator.repository.TenantRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Locale;

/**
 * Builds the PDF that stands for a document the tenant or the guarantor cannot provide: a sworn statement rendered by
 * {@link AttestationPdfTemplate}.
 * The statement is always a fixed sentence. The text typed by the tenant is only printed below it, as an explanation.
 */
@Service
@AllArgsConstructor
@Slf4j
public class EmptyBOPdfDocumentTemplate implements PdfTemplate<Document> {

    // Sentence sent by the tenant front for OTHER_RESIDENCY: the attestation states it in the first person instead
    static final String FRONT_OTHER_RESIDENCY_TEXT = "Le candidat indique qu'il n'est pas en mesure de fournir de quittance de logement pour ces 3 derniers mois";
    // Value sent by the tenant front when an optional explanation is left empty
    private static final String EMPTY_EXPLANATION = "-";

    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;
    private final TenantRepository tenantRepository;
    private final GuarantorRepository guarantorRepository;
    private final AttestationPdfTemplate attestationPdfTemplate;

    @Override
    public InputStream render(Document document) throws Exception {
        return attestationPdfTemplate.render(toAttestation(document));
    }

    private AttestationContent toAttestation(Document document) {
        DocumentCategory documentCategory = document.getDocumentCategory();
        if (documentCategory == DocumentCategory.FINANCIAL) {
            return financialAttestation(document);
        }
        if (documentCategory == DocumentCategory.RESIDENCY) {
            return residencyAttestation(document);
        }
        return taxAttestation(document);
    }

    private AttestationContent taxAttestation(Document document) {
        if (document.getDocumentSubCategory() == DocumentSubCategory.MY_PARENTS) {
            return attestation(document, "document.attestation.tax.parents", null);
        }
        if (document.getDocumentCategoryStep() == DocumentCategoryStep.TAX_NO_DECLARATION) {
            return attestation(document, "document.attestation.tax.nodeclaration", null);
        }
        if (document.getDocumentCategoryStep() == DocumentCategoryStep.TAX_NOT_RECEIVED) {
            return attestation(document, "document.attestation.tax.notreceived", null);
        }
        return attestation(document, "document.attestation.tax.other", document.getCustomText());
    }

    private AttestationContent financialAttestation(Document document) {
        if (document.getDocumentSubCategory() == DocumentSubCategory.NO_INCOME) {
            return attestation(document, "document.attestation.financial.noincome", document.getCustomText());
        }
        return attestation(document, "document.attestation.fallback", document.getCustomText());
    }

    private AttestationContent residencyAttestation(Document document) {
        if (isFrontOtherResidencyText(document.getCustomText())) {
            return attestation(document, "document.attestation.residency.other", null);
        }
        return attestation(document, "document.attestation.residency.noproof", document.getCustomText());
    }

    private static boolean isFrontOtherResidencyText(String customText) {
        if (customText == null) {
            return false;
        }
        String text = StringUtils.normalizeSpace(customText).replace('’', '\'');
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return FRONT_OTHER_RESIDENCY_TEXT.equalsIgnoreCase(text);
    }

    private AttestationContent attestation(Document document, String statementCode, String explanation) {
        String normalizedExplanation = StringUtils.normalizeSpace(explanation);
        if (EMPTY_EXPLANATION.equals(normalizedExplanation)) {
            normalizedExplanation = null;
        }
        String statement = messageSource.getMessage(statementCode, null, locale);
        String personName = StringUtils.normalizeSpace(getPersonName(document));
        if (StringUtils.isBlank(personName)) {
            log.warn("The attestation of document {} has no name", document.getId());
        }
        return new AttestationContent(personName, statement, normalizedExplanation);
    }

    private String getPersonName(Document document) {
        Long documentId = document.getId();
        return tenantRepository.getTenantByDocumentId(documentId).map(Tenant::getFullName)
                .orElseGet(() -> guarantorRepository.getGuarantorByDocumentId(documentId).map(Guarantor::getCompleteName).orElse("")
                );
    }

}
