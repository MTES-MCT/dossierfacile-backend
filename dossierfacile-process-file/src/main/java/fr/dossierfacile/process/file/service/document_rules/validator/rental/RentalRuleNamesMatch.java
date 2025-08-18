package fr.dossierfacile.process.file.service.document_rules.validator.rental;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RentalRuleNamesMatch extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_RENT_RECEIPT_NAME;
    }

    @Override
    protected boolean isValid(Document document) {
        if (checkNamesRule(document)) {
            return true;
        } else {
            log.info("Document names mismatches :{}", document.getId());
            return false;
        }
    }

    /**
     * Checks if names on each analysed file matches with the associated guarantor or to the tenant (more his partner for a couple).
     */
    // Should not happen because we have already checked that the document is parsed
    @SuppressWarnings("DataFlowIssue")
    private boolean checkNamesRule(Document document) {
        List<? extends Person> users = resolveUsers(document);

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            RentalReceiptFile parsedFile = (RentalReceiptFile) analysis.getParsedFile();

            String fullName = parsedFile.getTenantFullName().toUpperCase().replaceFirst("^(M. |MR |MME |MLLE |MONSIEUR |MADAME |MADEMOISELLE )", "");

            if (users.stream().noneMatch((user) -> PersonNameComparator.bearlyMixedEqualsTo(fullName, user.getLastName(), user.getFirstName())
                    || PersonNameComparator.bearlyMixedEqualsTo(fullName, user.getPreferredName(), user.getFirstName()))) {
                return false;
            }
        }
        return true;
    }

    private List<? extends Person> resolveUsers(Document document) {
        Tenant tenant = document.getTenant();
        if (tenant == null) {
            // Cas garant (pas de tenant directement sur le document)
            return List.of(document.getGuarantor());
        }
        ApartmentSharing sharing = tenant.getApartmentSharing();
        if (sharing != null && sharing.getApplicationType() == ApplicationType.COUPLE) {
            return sharing.getTenants();
        }
        return List.of(tenant);
    }
}
