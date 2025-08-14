package fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GuaranteeProviderNamesMatchTest {

    private File gpFile(List<GuaranteeProviderFile.FullName> names, ParsedStatus status) {
        GuaranteeProviderFile gpf = GuaranteeProviderFile.builder()
                .names(names)
                .status(status)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(gpf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private Document docWith(File file, Tenant tenant) {
        Guarantor guarantor = Guarantor.builder()
                .firstName("IGNORED")
                .lastName("IGNORED")
                .tenant(tenant)
                .build();
        return Document.builder()
                .guarantor(guarantor)
                .files(List.of(file))
                .build();
    }

    private RuleValidatorOutput validate(Document d) { return new GuaranteeProviderNamesMatch().validate(d); }

    @Test
    @DisplayName("OK si un nom/prénom correspond (lastName)")
    void match_last_name() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").preferredName("JDUP").build();
        File file = gpFile(List.of(new GuaranteeProviderFile.FullName("JEAN", "DUPONT")), ParsedStatus.COMPLETE);
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("OK si le lastName du fichier correspond au preferredName du locataire")
    void match_preferred_name() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("MARTIN").preferredName("DUPONT").build();
        File file = gpFile(List.of(new GuaranteeProviderFile.FullName("Jean", "Dupont")), ParsedStatus.COMPLETE);
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("KO si status INCOMPLETE")
    void incomplete_status() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").build();
        File file = gpFile(List.of(new GuaranteeProviderFile.FullName("JEAN", "DUPONT")), ParsedStatus.INCOMPLETE);
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("KO si aucun nom ne correspond")
    void no_name_matches() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").preferredName("JDUP").build();
        File file = gpFile(List.of(new GuaranteeProviderFile.FullName("PAUL", "MARTIN")), ParsedStatus.COMPLETE);
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("KO si fichier guarantee provider absent (parsedFileAnalysis null)")
    void missing_parsed_file_analysis() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").build();
        File file = File.builder().build(); // pas d'analyse => Optional.empty()
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("OK si un des plusieurs noms correspond")
    void multiple_names_one_matches() {
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").build();
        File file = gpFile(List.of(
                new GuaranteeProviderFile.FullName("PAUL", "MARTIN"),
                new GuaranteeProviderFile.FullName("JEAN", "DUPONT"),
                new GuaranteeProviderFile.FullName("ALAIN", "DURAND")
        ), ParsedStatus.COMPLETE);
        RuleValidatorOutput out = validate(docWith(file, tenant));
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

