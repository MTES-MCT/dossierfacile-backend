package fr.dossierfacile.process.file.service.document_rules.validator.payslip;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class PayslipRuleNamesMatch extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_PAYSLIP_NAME;
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

        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(() -> document.getGuarantor());
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            PayslipFile parsedFile = (PayslipFile) analysis.getParsedFile();

            String fullName = parsedFile.getFullname().toUpperCase().replaceFirst("^(M. |MR |MME |MLLE |MONSIEUR |MADAME |MADEMOISELLE )", "");

            if (!PersonNameComparator.bearlyMixedEqualsTo(fullName, documentOwner.getLastName(), documentOwner.getFirstName())
                    && !PersonNameComparator.bearlyMixedEqualsTo(fullName, documentOwner.getPreferredName(), documentOwner.getFirstName())) {
                return false;
            }
        }
        return true;
    }
}
