package fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import lombok.extern.slf4j.Slf4j;

import static fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider.GuaranteeProviderHelper.getGuaranteeProviderFile;

@Slf4j
public class GuaranteeProviderHasBeenParsed extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_GUARANTEE_PARSING;
    }

    @Override
    protected boolean isValid(Document document) {

        var guaranteeProviderFile = getGuaranteeProviderFile(document);
        return guaranteeProviderFile.isPresent();

    }
}