package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.PayslipNamesRuleData;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMultiMapper;
import fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model.PayslipNames;
import fr.dossierfacile.document.analysis.rule.validator.util.IdentityMatchUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/*
 * Rule R_PAYSLIP_NAME_MATCH:
 *
 * Cette regle verifie que l'identite extraite d'un bulletin de salaire correspond
 * a l'identite du locataire (ou garant selon le document porteur).
 *
 * Pourquoi cette logique est specifique:
 * - L'identite du bulletin est extraite par Document-IA sous forme de chaine libre
 *   (identityString).
 * - On ne dispose pas toujours d'un decoupage fiable nom/prenom directement exploitable.
 * - Les variantes usuelles doivent rester acceptees: accents, espaces, nom d'usage
 *   (preferredName), etc.
 *
 * Strategie de matching appliquee :
 * 1) Recuperation des analyses IA exploitables
 *    - On ne retient que les analyses IA en succes.
 *    - Si une analyse est absente ou non aboutie, la regle devient INCONCLUSIVE.
 * 2) Construction des donnees d'audit
 *    - On construit le nom attendu (prenom(s), nom, preferredName) depuis le dossier.
 *    - On trace egalement toutes les identites extraites du bulletin pour expliciter
 *      ce qui a ete compare dans la RuleData.
 * 3) Verification nom + prenom pour chaque bulletin extrait
 *    - Pour chaque identityString, on applique le moteur commun IdentityMatchUtil.
 *    - Le nom (lastName/preferredName) et le prenom doivent tous deux matcher.
 * 4) Decision finale
 *    - PASSED uniquement si toutes les identites extraites exploitables matchent.
 *    - FAILED des qu'une identite est manquante ou non correspondante.
 *    - INCONCLUSIVE si aucune extraction exploitable n'est disponible ou si l'identite
 *      attendue est absente.
 *
 * Intention metier :
 * - Maintenir une verification robuste de l'appartenance du bulletin au candidat,
 *   tout en limitant les faux negatifs lies aux variations d'ecriture des noms.
 */
@Slf4j
public class PayslipNamesRule extends BaseDocumentIAValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_NAME_MATCH;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        var nameToMatch = getNamesFromDocument(document);

        PayslipNamesRuleData.Name expectedName = null;
        if (nameToMatch != null) {
            expectedName = new PayslipNamesRuleData.Name(
                    nameToMatch.getFirstNamesAsString(),
                    nameToMatch.getLastName(),
                    nameToMatch.getPreferredName()
            );
        }
        PayslipNamesRuleData namesRuleData = new PayslipNamesRuleData(expectedName, new ArrayList<>());

        if (documentIAAnalyses.isEmpty() || hasAnyNonSuccessfulDocumentIAAnalyses(document)) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (nameToMatch == null) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        DocumentIAMultiMapper mapper = new DocumentIAMultiMapper();
        for (var analysis : documentIAAnalyses) {
            String identityString = mapper.map(List.of(analysis), PayslipNames.class)
                    .stream()
                    .findFirst()
                    .map(PayslipNames::getIdentityString)
                    .orElse(null);

            if (identityString == null) {
                namesRuleData = namesRuleData.addItem(new PayslipNamesRuleData.PayslipNamesEntry(
                        getFileId(analysis),
                        getFileName(analysis),
                        null
                ));
                continue;
            }

            List<String> identities = List.of(identityString);
            boolean lastNameMatches = IdentityMatchUtil.hasLastNameMatch(identities, nameToMatch);
            boolean firstNameMatches = IdentityMatchUtil.hasFirstNameMatch(identities, nameToMatch);

            if (!lastNameMatches || !firstNameMatches) {
                namesRuleData = namesRuleData.addItem(new PayslipNamesRuleData.PayslipNamesEntry(
                        getFileId(analysis),
                        getFileName(analysis),
                        identityString
                ));
            }
        }

        if (namesRuleData.payslipEntriesInError().isEmpty()) {
            return new RuleValidatorOutput(true, isBlocking(), DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), namesRuleData), RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    private String getFileName(DocumentIAFileAnalysis analysis) {
        if (analysis.getFile() == null || analysis.getFile().getStorageFile() == null) {
            return null;
        }
        return analysis.getFile().getStorageFile().getName();
    }

    private Long getFileId(DocumentIAFileAnalysis analysis) {
        if (analysis.getFile() == null) {
            return null;
        }
        return analysis.getFile().getId();
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
