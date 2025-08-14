package fr.dossierfacile.process.file.service.documentrules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static fr.dossierfacile.process.file.service.documentrules.validator.scholarship.ScholarShipHelper.getOptionalValue;

@Slf4j
public class ScholarshipRuleNamesMatch extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_SCHOLARSHIP_NAME;
    }

    @Override
    protected boolean isValid(Document document) {
        var scholarshipFile = getOptionalValue(document);
        if (scholarshipFile.isEmpty()) {
            log.info("Scholarship file not found for document: {}", document.getId());
            return false; // If no scholarship file, we consider the rule invalid
        }
        var safeScholarshipFile = scholarshipFile.get();
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(document::getGuarantor);

        return PersonNameComparator.bearlyEqualsTo(safeScholarshipFile.getFirstName(), documentOwner.getFirstName())
                && (PersonNameComparator.bearlyEqualsTo(safeScholarshipFile.getLastName(), documentOwner.getLastName())
                || PersonNameComparator.bearlyEqualsTo(safeScholarshipFile.getLastName(), documentOwner.getPreferredName()));
    }
}
