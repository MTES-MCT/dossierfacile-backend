package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.process.file.service.document_rules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.blurry.BlurryRuleHasBeenAnalysedBI;
import fr.dossierfacile.process.file.service.document_rules.validator.blurry.BlurryRuleIsNotBlurry;
import fr.dossierfacile.process.file.service.document_rules.validator.blurry.BlurryRuleNotAllBlankBI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlurryRulesValidationService extends AbstractRulesValidationService {

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getFiles().stream().allMatch(file -> file.getBlurryFileAnalysis() != null)
                && !CollectionUtils.isEmpty(document.getFiles());
    }

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators() {
        return List.of(
                new BlurryRuleHasBeenAnalysedBI(),
                new BlurryRuleNotAllBlankBI(),
                new BlurryRuleIsNotBlurry()
        );
    }
}
