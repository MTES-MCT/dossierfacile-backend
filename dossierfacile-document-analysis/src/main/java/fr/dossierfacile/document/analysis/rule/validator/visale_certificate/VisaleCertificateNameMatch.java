package fr.dossierfacile.document.analysis.rule.validator.visale_certificate;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;
import fr.dossierfacile.document.analysis.rule.validator.visale_certificate.document_ia_model.VisaleBeneficiaire;
import fr.dossierfacile.document.analysis.rule.validator.visale_certificate.document_ia_model.VisaleCertificate;
import fr.dossierfacile.document.analysis.util.NameUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/*
 * Rule R_VISALE_CERTIFICATE_NAME_MATCH:
 *
 * Cette règle vérifie que l'identité du bénéficiaire figurant sur le certificat
 * Visale (extrait par Document-IA) correspond à l'identité du locataire (ou de son conjoint,
 * en cas de dossier COUPLE).
 *
 * Pourquoi cette logique est spécifique:
 * - Un certificat Visale peut couvrir un couple : il faut alors accepter que le
 *   bénéficiaire mentionné soit le conjoint et non le locataire principal.
 * - Les champs extraits (prénom(s) et nom) peuvent présenter des variantes
 *   orthographiques, des accents manquants ou des tirets/apostrophes.
 * - On doit aussi accepter le nom d'usage (preferredName) comme alternative au nom
 *   de naissance.
 *
 * Stratégie de matching appliquée :
 * 1) Construction des identités candidates côté dossier
 *    - Locataire principal toujours inclus (lastName + preferredName + firstName).
 *    - Si le type de dossier est COUPLE, le conjoint est également ajouté à la liste
 *      des candidats.
 * 2) Extraction des bénéficiaires côté document
 *    - On fusionne les analyses IA disponibles via DocumentIAMergerMapper.
 *    - On ne retient que les bénéficiaires dont les champs sont valides (isValid()).
 * 3) Matching strict (NameUtil.isNameMatching)
 *    - Normalisation des chaînes (trim, uppercase, suppression des accents).
 *    - Vérification de la présence du nom dossier dans le nom extrait, et
 *      réciproquement pour les prénoms.
 * 4) Fallback fuzzy (distance de Levenshtein token par token)
 *    - Si le matching strict échoue, on découpe chaque champ (nom / prénom) en tokens
 *      (séparateurs : espaces, tirets, apostrophes).
 *    - On n'applique Levenshtein qu'aux tokens d'au moins MIN_LENGTH_FOR_LEVENSHTEIN
 *      caractères pour éviter les faux positifs sur les noms courts.
 *    - Un match est retenu si la distance ≤ MAX_LEVENSHTEIN_DISTANCE pour au moins
 *      un token nom ET un token prénom.
 * 5) Décision finale
 *    - PASSED si au moins un candidat dossier correspond à au moins un bénéficiaire
 *      extrait (strict ou fuzzy).
 *    - FAILED si aucune correspondance n'est trouvée.
 *    - INCONCLUSIVE si aucune analyse IA n'est disponible, si la liste des candidats
 *      est vide, ou si le certificat extrait ne contient aucun bénéficiaire exploitable.
 *
 * Intention métier :
 * - Limiter les faux négatifs dus aux variantes de saisie tout en garantissant une
 *   vérification croisée nom + prénom, afin de s'assurer que le certificat VISALE
 *   appartient bien au candidat.
 */
@Slf4j
public class VisaleCertificateNameMatch extends BaseDocumentIAValidator {
    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;
    private static final int MIN_LENGTH_FOR_LEVENSHTEIN = 4;

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
        return DocumentRule.R_VISALE_CERTIFICATE_NAME_MATCH;
    }

    @Override
    public RuleValidatorOutput validate(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);

        // Get the names to match (tenant or co-tenants in a couple)
        List<DocumentIdentity> namesToMatch = getNamesToMatchFromDocument(document);

        NamesRuleData namesRuleData = buildExpectedNamesRuleData(namesToMatch);

        if (documentIAAnalyses.isEmpty()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        if (namesToMatch.isEmpty()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        var extractedCertificate = new DocumentIAMergerMapper().map(documentIAAnalyses, VisaleCertificate.class);

        if (extractedCertificate.isEmpty() || !extractedCertificate.get().hasValidBeneficiaires()) {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentInconclusiveRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.INCONCLUSIVE
            );
        }

        VisaleCertificate certificate = extractedCertificate.get();
        List<VisaleBeneficiaire> beneficiaires = certificate.beneficiaires;

        // Build the list of extracted names for the rule data
        List<NamesRuleData.Name> extractedNames = beneficiaires.stream()
                .filter(VisaleBeneficiaire::isValid)
                .map(b -> new NamesRuleData.Name(
                        String.join(" ", b.getFirstNames()),
                        b.getLastName(),
                        null
                ))
                .toList();

        namesRuleData = new NamesRuleData(namesRuleData.expectedName(), extractedNames);

        // Check if any of the names to match corresponds to any beneficiary
        boolean isNameMatch = namesToMatch.stream().anyMatch(nameToMatch ->
                beneficiaires.stream()
                        .filter(VisaleBeneficiaire::isValid)
                        .anyMatch(beneficiaire -> isNameMatchingWithFallback(nameToMatch, beneficiaire))
        );

        if (isNameMatch) {
            return new RuleValidatorOutput(
                    true,
                    isBlocking(),
                    DocumentAnalysisRule.documentPassedRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.PASSED
            );
        } else {
            return new RuleValidatorOutput(
                    false,
                    isBlocking(),
                    DocumentAnalysisRule.documentFailedRuleFromWithData(getRule(), namesRuleData),
                    RuleValidatorOutput.RuleLevel.FAILED
            );
        }
    }

    /**
     * Gets the names to match from the document's tenant and co-tenants (in case of a couple).
     * For a COUPLE application type, both tenants' names should be considered.
     */
    private List<DocumentIdentity> getNamesToMatchFromDocument(Document document) {
        List<DocumentIdentity> names = new ArrayList<>();

        Tenant tenant = document.getGuarantor().getTenant();
        if (tenant != null) {
            names.add(new DocumentIdentity(
                    List.of(tenant.getFirstName()),
                    tenant.getLastName(),
                    tenant.getPreferredName()
            ));

            // For couples, also include the co-tenant's name
            if (tenant.getApartmentSharing() != null
                    && tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
                for (Tenant coTenant : tenant.getApartmentSharing().getTenants()) {
                    if (!coTenant.getId().equals(tenant.getId())) {
                        names.add(new DocumentIdentity(
                                List.of(coTenant.getFirstName()),
                                coTenant.getLastName(),
                                coTenant.getPreferredName()
                        ));
                    }
                }
            }
        }

        return names;
    }

    /**
     * Builds the expected names rule data from the list of names to match.
     * For simplicity, we use the first name as the expected name.
     */
    private NamesRuleData buildExpectedNamesRuleData(List<DocumentIdentity> namesToMatch) {
        if (namesToMatch.isEmpty()) {
            return new NamesRuleData((NamesRuleData.Name) null, List.of());
        }

        // Use the first name as the primary expected name
        DocumentIdentity first = namesToMatch.get(0);
        NamesRuleData.Name expectedName = new NamesRuleData.Name(
                first.getFirstNamesAsString(),
                first.getLastName(),
                first.getPreferredName()
        );

        return new NamesRuleData(expectedName, List.of());
    }

    private boolean isNameMatchingWithFallback(DocumentIdentity nameToMatch, VisaleBeneficiaire beneficiaire) {
        if (NameUtil.isNameMatching(nameToMatch, beneficiaire)) {
            return true;
        }
        return hasFuzzyTokenMatch(
            identityTokens(nameToMatch.getFirstNames()),
            identityTokens(beneficiaire.getFirstNames())
        ) && hasFuzzyTokenMatch(
            identityTokens(lastNameTokens(nameToMatch.getLastName(), nameToMatch.getPreferredName())),
            identityTokens(lastNameTokens(beneficiaire.getLastName(), beneficiaire.getPreferredName()))
        );
    }

    private List<String> lastNameTokens(String lastName, String preferredName) {
        return Stream.of(lastName, preferredName)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> identityTokens(List<String> identity) {
        return identity.stream()
                .filter(Objects::nonNull)
                .flatMap(this::splitTokens)
                .map(NameUtil::sanitizeForComparison)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private Stream<String> splitTokens(String value) {
        return TOKEN_SEPARATOR.splitAsStream(value.replaceAll("[-'_]", " "))
            .filter(Objects::nonNull)
            .filter(token -> !token.isBlank());
    }

    private boolean hasFuzzyTokenMatch(List<String> expectedTokens, List<String> extractedTokens) {
        if (expectedTokens.isEmpty() || extractedTokens.isEmpty()) {
            return false;
        }
        LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
        return expectedTokens.stream()
                .filter(token -> token.length() >= MIN_LENGTH_FOR_LEVENSHTEIN)
                .anyMatch(expectedToken -> extractedTokens.stream()
                        .filter(token -> token.length() >= MIN_LENGTH_FOR_LEVENSHTEIN)
                        .anyMatch(extractedToken -> {
                            Integer distance = levenshtein.apply(expectedToken, extractedToken);
                            return distance != null && distance <= MAX_LEVENSHTEIN_DISTANCE;
                        }));
    }

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
