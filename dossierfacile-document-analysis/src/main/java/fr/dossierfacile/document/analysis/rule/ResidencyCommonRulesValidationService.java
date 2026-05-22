package fr.dossierfacile.document.analysis.rule;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.document.analysis.rule.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.HasBeenDocumentIAAnalysedBI;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.AtLeastOneClassificationValidatorB;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResidencyCommonRulesValidationService extends AbstractRulesValidationService {

    @Override
    List<AbstractDocumentRuleValidator> getDocumentRuleValidators(Document document) {
        return List.of(
                new HasBeenDocumentIAAnalysedBI(),
                new AtLeastOneClassificationValidatorB(
                        List.of(
                                "quittance_loyer",
                                "facture_energie",
                                "attestation_contrat_energie",
                                "attestation_hebergement",
                                "taxe_fonciere"
                        )
                )
        );
    }
}
