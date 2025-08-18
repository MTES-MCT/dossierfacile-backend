package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class IncomeTaxRuleNamesMatch extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_TAX_NAMES;
    }

    @Override
    protected boolean isValid(Document document) {
        // Firstname LastName PreferredName
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
        String firstName = documentOwner.getFirstName();
        String lastName = documentOwner.getLastName();
        String preferredName = documentOwner.getPreferredName();
        for (File dfFile : document.getFiles()) {
            BarCodeFileAnalysis analysis = dfFile.getFileAnalysis();
            if (analysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT) {
                Optional<TaxIncomeMainFile> qrDocument = IncomeTaxHelper.fromQR(dfFile.getFileAnalysis());
                if (qrDocument.isEmpty()) {
                    log.info("Le document n'est pas un avis d'imposition docId:{}", document.getId());
                    return false;
                }
                var safeQrDocument = qrDocument.get();
                if (!((PersonNameComparator.bearlyEqualsTo(safeQrDocument.getDeclarant1Nom(), lastName, firstName)
                        || PersonNameComparator.bearlyEqualsTo(safeQrDocument.getDeclarant1Nom(), preferredName, firstName))
                        || (safeQrDocument.getDeclarant2Nom() != null &&
                        (PersonNameComparator.bearlyEqualsTo(safeQrDocument.getDeclarant2Nom(), lastName, firstName)
                                || PersonNameComparator.bearlyEqualsTo(safeQrDocument.getDeclarant2Nom(), preferredName, firstName)))
                )) {
                    log.info("Le nom/prenom ne correpond pas à l'utilisateur docId:{} firstname: {}", document.getId(), firstName);
                    return false;
                }
            }
        }
        return true;

    }
}