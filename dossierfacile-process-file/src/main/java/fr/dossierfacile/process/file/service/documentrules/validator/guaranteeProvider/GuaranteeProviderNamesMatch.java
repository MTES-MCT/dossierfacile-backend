package fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ParsedStatus;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;

import static fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderHelper.getGuaranteeProviderFile;

@Slf4j
public class GuaranteeProviderNamesMatch extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_GUARANTEE_NAMES;
    }

    @Override
    protected boolean isValid(Document document) {
        Tenant tenant = document.getGuarantor().getTenant();
        var parsedFile = getGuaranteeProviderFile(document);
        if (parsedFile.isEmpty()) {
            log.warn("Guarantee provider file is empty for document {}", document.getId());
            return false;
        }

        var safeParsedFile = parsedFile.get();

        if (safeParsedFile.getStatus() == ParsedStatus.INCOMPLETE) {
            return false;
        }
        return safeParsedFile.getNames().stream().anyMatch(
                (fullname) -> PersonNameComparator.bearlyEqualsTo(fullname.firstName(), tenant.getFirstName())
                        && (PersonNameComparator.bearlyEqualsTo(fullname.lastName(), tenant.getLastName())
                        || PersonNameComparator.bearlyEqualsTo(fullname.lastName(), tenant.getPreferredName())));
    }
}
