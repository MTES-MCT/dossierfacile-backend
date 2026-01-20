package fr.dossierfacile.api.front.service.document.analysis;

import fr.dossierfacile.api.front.service.document.analysis.rule.AbstractRulesValidationService;
import fr.dossierfacile.api.front.service.document.analysis.rule.BulletinSalaireRulesValidationService;
import fr.dossierfacile.api.front.service.document.analysis.rule.CarteNationalIdentiteRulesValidationService;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class DocumentAnalysisServiceConfiguration {

    @Bean
    public Map<DocumentSubCategory, AbstractRulesValidationService> documentSubCategoryValidatorMap(
            CarteNationalIdentiteRulesValidationService carteNationalIdentiteRulesValidationService,
            BulletinSalaireRulesValidationService BulletinSalaireRulesValidationService
    ) {
        EnumMap<DocumentSubCategory, AbstractRulesValidationService> validators = new EnumMap<>(DocumentSubCategory.class);
        validators.put(DocumentSubCategory.FRENCH_IDENTITY_CARD, carteNationalIdentiteRulesValidationService);
        validators.put(DocumentSubCategory.SALARY, BulletinSalaireRulesValidationService); // Placeholder for other validators
        return validators;
    }
}
