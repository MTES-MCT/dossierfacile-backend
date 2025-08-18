package fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.enums.ParsedStatus;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider.GuaranteeProviderHelper.getGuaranteeProviderFile;

@Slf4j
public class GuaranteeProviderRuleYearValidity extends AbstractDocumentRuleValidator {

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
        return DocumentRule.R_GUARANTEE_EXPIRED;
    }

    @Override
    protected boolean isValid(Document document) {
        var parsedFile = getGuaranteeProviderFile(document);
        if (parsedFile.isEmpty()) {
            log.warn("Guarantee provider file is empty for document {}", document.getId());
            return false;
        }

        var safeParsedFile = parsedFile.get();

        if (safeParsedFile.getStatus() == ParsedStatus.INCOMPLETE) {
            return false;
        }

        LocalDate validityDate = LocalDate.parse(safeParsedFile.getValidityDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return validityDate.isAfter(LocalDate.now());
    }
}
