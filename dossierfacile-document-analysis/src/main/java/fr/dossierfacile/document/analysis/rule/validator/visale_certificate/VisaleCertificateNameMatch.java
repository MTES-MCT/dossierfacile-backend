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

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VisaleCertificateNameMatch extends BaseDocumentIAValidator {

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
                        .anyMatch(beneficiaire -> NameUtil.isNameMatching(nameToMatch, beneficiaire))
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

        Tenant tenant = document.getTenant();
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

    @Override
    protected boolean isValid(Document document) {
        return false;
    }
}
