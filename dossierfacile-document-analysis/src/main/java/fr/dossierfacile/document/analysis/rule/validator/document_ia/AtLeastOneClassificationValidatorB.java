package fr.dossierfacile.document.analysis.rule.validator.document_ia;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;

import java.util.List;

/*
 * Rule R_DOCUMENT_IA_CLASSIFICATION (variant B - at least one):
 *
 * Cette variante valide le document si au moins une analyse Document-IA
 * réussie contient une classification dont le documentType appartient à la
 * liste autorisée passée au constructeur.
 *
 * Comportement:
 * - FAILED si aucune analyse IA SUCCESS n'est disponible.
 * - PASSED dès qu'une classification autorisée est trouvée.
 * - FAILED sinon (analyses présentes mais aucune classification attendue).
 *
 * Différence avec ClassificationValidatorB:
 * - ClassificationValidatorB attend que toutes les classifications exploitables
 *   soient conformes.
 * - AtLeastOneClassificationValidatorB n'exige qu'une seule correspondance.
 */
public class AtLeastOneClassificationValidatorB extends BaseDocumentIAValidator {

    private final List<String> documentTypes;

    public AtLeastOneClassificationValidatorB(List<String> documentTypes) {
        this.documentTypes = documentTypes;
    }

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_DOCUMENT_IA_CLASSIFICATION;
    }

    @Override
    protected boolean isValid(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        if (documentIAAnalyses.isEmpty()) {
            return false;
        }
        var oneGoodClassification = false;
        for (var analysis : documentIAAnalyses) {
            var classification = analysis.getResult().getClassification();
            if (classification != null && documentTypes.contains(classification.getDocumentType())) {
                oneGoodClassification = true;
                break;
            }
        }
        return oneGoodClassification;
    }
}