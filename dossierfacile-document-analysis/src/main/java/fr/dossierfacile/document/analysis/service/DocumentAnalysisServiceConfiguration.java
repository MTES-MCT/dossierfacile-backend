package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.AbstractRulesValidationService;
import fr.dossierfacile.document.analysis.rule.AvisImpositionRulesValidationService;
import fr.dossierfacile.document.analysis.rule.BulletinSalaireRulesValidationService;
import fr.dossierfacile.document.analysis.rule.CarteNationalIdentiteRulesValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class DocumentAnalysisServiceConfiguration {

    @Bean
    public Map<DocumentSubCategory, AbstractRulesValidationService> documentSubCategoryValidatorMap(
            CarteNationalIdentiteRulesValidationService carteNationalIdentiteRulesValidationService,
            BulletinSalaireRulesValidationService bulletinSalaireRulesValidationService,
            AvisImpositionRulesValidationService avisImpositionRulesValidationService
    ) {
        EnumMap<DocumentSubCategory, AbstractRulesValidationService> validators = new EnumMap<>(DocumentSubCategory.class);
        validators.put(DocumentSubCategory.FRENCH_IDENTITY_CARD, carteNationalIdentiteRulesValidationService);
        validators.put(DocumentSubCategory.SALARY, bulletinSalaireRulesValidationService);
        validators.put(DocumentSubCategory.MY_NAME, avisImpositionRulesValidationService);
        return validators;
    }
}
