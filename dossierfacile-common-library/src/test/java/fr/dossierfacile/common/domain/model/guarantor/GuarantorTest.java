package fr.dossierfacile.common.domain.model.guarantor;

import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.infrastructure.entity.GuarantorEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuarantorTest {

    @Test
    void should_get_complete_name_for_natural_person() {
        GuarantorEntity entity = GuarantorEntity.builder()
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .firstName("Jean")
                .lastName("Dupont")
                .build();
        Guarantor guarantor = new Guarantor(entity);

        assertThat(guarantor.getCompleteName()).isEqualTo("Jean Dupont");
    }

    @Test
    void should_get_complete_name_for_legal_person() {
        GuarantorEntity entity = GuarantorEntity.builder()
                .typeGuarantor(TypeGuarantor.LEGAL_PERSON)
                .legalPersonName("Societe SARL")
                .build();
        Guarantor guarantor = new Guarantor(entity);

        assertThat(guarantor.getCompleteName()).isEqualTo("Societe SARL");
    }

    @Test
    void should_get_empty_name_when_no_names_present() {
        GuarantorEntity entity = GuarantorEntity.builder()
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .build();
        Guarantor guarantor = new Guarantor(entity);

        assertThat(guarantor.getCompleteName()).isEmpty();
    }
}
