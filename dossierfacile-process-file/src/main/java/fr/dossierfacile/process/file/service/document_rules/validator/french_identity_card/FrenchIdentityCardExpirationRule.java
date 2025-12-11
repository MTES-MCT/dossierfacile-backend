package fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card.document_ia_model.DocumentExpiration;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class FrenchIdentityCardExpirationRule extends BaseDocumentIAValidator {

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
        return DocumentRule.R_FRENCH_IDENTITY_CARD_EXPIRATION;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        var isCardValid = Optional.of(false);

        var extractedDates = DocumentIAMergerMapper.map(documentIAAnalyses, DocumentExpiration.class);

        if (extractedDates.isPresent()) {
            isCardValid = isIdentityCardValid(extractedDates.get());
        } else {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        if (isCardValid.isEmpty()) {
            return new RuleValidatorOutput(false, isBlocking(), DocumentAnalysisRule.documentInconclusiveRuleFrom(getRule()), RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        }

        boolean cardValid = isCardValid.orElse(false);

        if (cardValid) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFrom(getRule()),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFrom(getRule()),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    // We return Optional<Boolean> to be able to return empty when we don't have enough data to decide
    private Optional<Boolean> isIdentityCardValid(DocumentExpiration dates) {
        if (dates.expirationDate == null) {
            return Optional.empty();
        }

        // The card is valid;
        if (dates.expirationDate.isAfter(LocalDate.now())) {
            return Optional.of(true);
        }

        if (dates.cardNumber == null) {
            // We don't have the card number but we decided the card is expired !
            return Optional.of(false);
        }

        // Old card we need complex algorithm
        // Old card has 12 numeric characters without letters
        // New card has alphanumeric characters with at least one letter
        boolean isNewCard = Pattern.compile("[A-Z]").matcher(dates.cardNumber).find();

        if (isNewCard) {
            // New card and expired
            // The card is expired
            return Optional.of(false);
        }

        // Old card : we need the complex algorithm
        // We have all the information to decide !
        if (dates.birthDate != null && dates.deliveryDate != null) {
            boolean valid = isIdentityCardValidComplexAlgorithm(
                    dates.deliveryDate,
                    dates.expirationDate,
                    dates.birthDate
            );
            return Optional.of(valid);
        }

        // Not enough data to decide
        return Optional.empty();
    }

    // Only called when identity card is an old one and the extraction returned all dates
    private boolean isIdentityCardValidComplexAlgorithm(LocalDate deliveryDate, LocalDate expirationDate, LocalDate birthDate) {
        var today = LocalDate.now();

        long ageAtDelivery = ChronoUnit.YEARS.between(birthDate, deliveryDate);
        LocalDate realExpirationDate = expirationDate;

        // Règle : Si majeur (>= 18 ans) au moment de la délivrance, on ajoute 5 ans
        if (ageAtDelivery >= 18) {
            realExpirationDate = expirationDate.plusYears(5);
        }

        // Étape 4 : Vérification finale
        // La carte est valide si la date du jour n'est PAS après la date d'expiration réelle
        return !today.isAfter(realExpirationDate);
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
