package fr.dossierfacile.process.file.service.document_rules.validator.france_identite;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static fr.dossierfacile.process.file.util.NameUtil.normalizeName;

@Slf4j
public class FranceIdentiteRuleNamesMatch extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_FRANCE_IDENTITE_NAMES;
    }

    @Override
    protected boolean isValid(Document document) {
        var parsedFile = FranceIdentiteHelper.getOptionalValue(document);
        if (parsedFile.isEmpty()) {
            return false;
        }
        if (checkNamesRule(parsedFile.get(), document)) {
            return true;
        } else {
            log.info("Document names mismatches :{}", document.getId());
            return false;
        }
    }

    // Should not happen because we have already checked that the document is parsed
    private boolean checkNamesRule(FranceIdentiteApiResult parsedDocument, Document document) {
        if (parsedDocument == null || parsedDocument.getAttributes() == null) {
            return false;
        }
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);
        String firstName = documentOwner.getFirstName();
        String lastName = documentOwner.getLastName();
        return normalizeName(parsedDocument.getAttributes().getGivenName()).contains(normalizeName(firstName))
                && normalizeName(parsedDocument.getAttributes().getFamilyName()).contains(normalizeName(lastName));
    }
}
