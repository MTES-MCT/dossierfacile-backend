package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.rental.RentalHasBeenParsedBI;
import fr.dossierfacile.process.file.service.document_rules.validator.rental.RentalRuleMonthValidity;
import fr.dossierfacile.process.file.service.document_rules.validator.rental.RentalRuleNamesMatch;
import fr.dossierfacile.process.file.service.document_rules.validator.rental.RentalRuleNumberOfPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalReceiptRulesValidationService extends AbstractRulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.RESIDENCY
                && document.getDocumentSubCategory() == DocumentSubCategory.TENANT
                && !CollectionUtils.isEmpty(document.getFiles());
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new RentalRuleNumberOfPage(),
                new RentalHasBeenParsedBI(),
                new RentalRuleNamesMatch(),
                new RentalRuleMonthValidity()
        );
    }
}