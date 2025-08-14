package fr.dossierfacile.process.file.service.documentrules.validator.rental;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

public class RentalRuleNamesMatchTest {

    private File buildReceipt(String tenantFullName) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(RentalReceiptFile.builder()
                        .tenantFullName(tenantFullName)
                        .period(YearMonth.now())
                        .build())
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document doc) {
        return new RentalRuleNamesMatch().validate(doc);
    }

    @Test
    void single_tenant_name_matches() {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(buildReceipt("Jean Dupont")))
                .build();

        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_NAME);
    }

    @Test
    void single_tenant_with_multiple_first_name_matches() {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .firstName("Jean François")
                .lastName("Dupont")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(buildReceipt("Jean Dupont")))
                .build();

        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_NAME);
    }

    @Test
    void single_tenant_preferred_name_matches() {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .firstName("Jean-François")
                .preferredName("Jeff")
                .lastName("Dupont")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(buildReceipt("Jean-François Jeff")))
                .build();

        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void couple_application_name_of_partner_matches() {
        // ApartmentSharing with COUPLE
        ApartmentSharing sharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .build();
        Tenant tenant1 = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .apartmentSharing(sharing)
                .build();
        Tenant tenant2 = Tenant.builder()
                .firstName("Marie")
                .lastName("Martin")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant1, tenant2));

        Document doc = Document.builder()
                .tenant(tenant1) // document rattaché au premier
                .files(List.of(buildReceipt("Marie Martin")))
                .build();

        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void couple_application_fails_if_name_not_in_any_partner() {
        ApartmentSharing sharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .build();
        Tenant tenant1 = Tenant.builder().firstName("Jean").lastName("Dupont").apartmentSharing(sharing).build();
        Tenant tenant2 = Tenant.builder().firstName("Marie").lastName("Martin").apartmentSharing(sharing).build();
        sharing.setTenants(List.of(tenant1, tenant2));

        Document doc = Document.builder()
                .tenant(tenant1)
                .files(List.of(buildReceipt("Paul Durant")))
                .build();

        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void guarantor_document_name_matches() {
        Guarantor guarantor = Guarantor.builder()
                .firstName("Luc")
                .lastName("Bernard")
                .build();
        Document doc = Document.builder()
                .guarantor(guarantor)
                .files(List.of(buildReceipt("Luc Bernard")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void prefixes_are_removed() {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .firstName("Sophie")
                .lastName("Durand")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(buildReceipt("Mme Sophie Durand"), buildReceipt("Mademoiselle Sophie Durand")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void mismatch_due_to_firstname_difference() {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .firstName("Alain")
                .lastName("Robert")
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(buildReceipt("Jean Robert")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}
