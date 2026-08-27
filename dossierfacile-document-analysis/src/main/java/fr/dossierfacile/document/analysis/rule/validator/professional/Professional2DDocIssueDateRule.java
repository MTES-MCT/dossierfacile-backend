package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.rule.ExpirationRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*
 * Rule R_PROFESSIONAL_2DDOC_ISSUE_DATE (Professional 2D-Doc issue date rule):
 *
 * Cette règle vérifie la date d'émission (`issue_date`) du 2D-Doc (type `29`) présent sur le document professionnel.
 *
 * Données examinées:
 * - La propriété `issue_date` du premier 2D-Doc de type `29` trouvé dans les analyses Document-IA réussies.
 *
 * Fonctionnement:
 * - Filtre et récupère la date d'émission (`issue_date`) du premier barcode 2D-Doc de type `29`.
 * - Si aucune analyse IA réussie, aucun 2D-Doc de type `29` ou aucune date d'émission n'est disponible -> retourne INCONCLUSIVE.
 * - Vérifie que la date d'émission est située entre il y a 2 mois et aujourd'hui (`LocalDate.now(clock)`).
 * - Si la date d'émission respecte cet intervalle -> retourne PASSED avec ExpirationRuleData.
 * - Si la date d'émission est antérieure à 2 mois ou située dans le futur -> retourne FAILED avec ExpirationRuleData.
 * - Le seuil de 2 mois est configurable via la variable statique `MAXIMUM_AGE_IN_MONTHS`.
 */
public class Professional2DDocIssueDateRule extends BaseDocumentIAValidator {

    public static final int MAXIMUM_AGE_IN_MONTHS = 2;
    private static final String EXPECTED_DOC_TYPE = "29";

    private final Clock clock;

    public Professional2DDocIssueDateRule() {
        this(Clock.systemDefaultZone());
    }

    public Professional2DDocIssueDateRule(Clock clock) {
        this.clock = clock;
    }

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
        return DocumentRule.R_PROFESSIONAL_2DDOC_ISSUE_DATE;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        var issueDateOpt = extractIssueDate(documentIAAnalyses);

        if (issueDateOpt.isEmpty()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), null),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        LocalDate issueDate = issueDateOpt.get();
        ExpirationRuleData ruleData = new ExpirationRuleData(issueDate);

        if (isIssueDateValid(issueDate)) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), ruleData),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), ruleData),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    private Optional<LocalDate> extractIssueDate(List<DocumentIAFileAnalysis> documentIAAnalyses) {
        return documentIAAnalyses.stream()
                .map(DocumentIAFileAnalysis::getResult)
                .filter(Objects::nonNull)
                .filter(result -> result.getBarcodes() != null)
                .flatMap(result -> result.getBarcodes().stream())
                .filter(Objects::nonNull)
                .filter(barcode -> EXPECTED_DOC_TYPE.equals(barcode.getDocType()))
                .map(BarcodeModel::getIssueDate)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean isIssueDateValid(LocalDate issueDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate threshold = today.minusMonths(MAXIMUM_AGE_IN_MONTHS);
        return !issueDate.isBefore(threshold) && !issueDate.isAfter(today);
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
