package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarshipRuleAmountValidity;
import fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarshipRuleHasBeenParsed;
import fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarshipRuleNamesMatch;
import fr.dossierfacile.process.file.service.document_rules.validator.scholarship.ScholarshipRuleYearValidity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScholarshipRulesValidationService extends AbstractRulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.FINANCIAL
                && document.getDocumentSubCategory() == DocumentSubCategory.SCHOLARSHIP
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null
                && f.getParsedFileAnalysis().getParsedFile().getClassification() == ParsedFileClassification.SCHOLARSHIP);
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new ScholarshipRuleHasBeenParsed(),
                new ScholarshipRuleNamesMatch(),
                new ScholarshipRuleYearValidity(),
                new ScholarshipRuleAmountValidity()
        );
    }

}