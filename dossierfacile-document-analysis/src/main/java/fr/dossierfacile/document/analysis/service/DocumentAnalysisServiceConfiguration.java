package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DocumentAnalysisServiceConfiguration {

    @Bean
    public Map<DocumentSubCategory, AbstractRulesValidationService> documentSubCategoryValidatorMap(
            CarteNationalIdentiteRulesValidationService carteNationalIdentiteRulesValidationService,
            BulletinSalaireRulesValidationService bulletinSalaireRulesValidationService,
            AvisImpositionRulesValidationService avisImpositionRulesValidationService,
            VisaleCertificateRulesValidationService visaleCertificateRulesValidationService
    ) {
        EnumMap<DocumentSubCategory, AbstractRulesValidationService> validators = new EnumMap<>(DocumentSubCategory.class);
        validators.put(DocumentSubCategory.FRENCH_IDENTITY_CARD, carteNationalIdentiteRulesValidationService);
        validators.put(DocumentSubCategory.SALARY, bulletinSalaireRulesValidationService);
        validators.put(DocumentSubCategory.MY_NAME, avisImpositionRulesValidationService);
        validators.put(DocumentSubCategory.VISALE, visaleCertificateRulesValidationService);

        // Property tax notice (taxe foncière, document category RESIDENCY/OWNER). has its own dedicated rules with strict classification.
        validators.put(DocumentSubCategory.OWNER, new PropertyTaxRulesValidationService());

        // Other residency documents (TENANT, GUEST) keep the common, lenient classification service.
        var residencyAnalysisService = new ResidencyCommonRulesValidationService();
        List.of(DocumentSubCategory.TENANT, DocumentSubCategory.GUEST)
                .forEach(it -> validators.put(it, residencyAnalysisService));

        return validators;
    }
}
