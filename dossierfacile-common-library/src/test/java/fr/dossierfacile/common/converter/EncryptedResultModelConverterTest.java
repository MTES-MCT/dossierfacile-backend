package fr.dossierfacile.common.converter;

import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedResultModelConverterTest {

    private static final String PROP_KEY_NAME = "dossierfacile.document.ia.result.encryption.key";

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);
        System.setProperty(PROP_KEY_NAME, keyBase64);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(PROP_KEY_NAME);
    }

    @Test
    void should_round_trip_result_model() {
        EncryptedResultModelConverter converter = new EncryptedResultModelConverter();

        ResultModel input = ResultModel.builder()
                .extraction(ExtractionModel.builder()
                        .properties(List.of(
                                GenericProperty.builder()
                                        .name("nom_test")
                                        .type(GenericProperty.TYPE_STRING)
                                        .value("Dupont")
                                        .build()
                        ))
                        .build())
                .build();

        String encrypted1 = converter.convertToDatabaseColumn(input);
        String encrypted2 = converter.convertToDatabaseColumn(input);

        assertThat(encrypted1).isNotNull().startsWith("v1:");
        assertThat(encrypted2).isNotNull().startsWith("v1:").isNotEqualTo(encrypted1);

        ResultModel output1 = converter.convertToEntityAttribute(encrypted1);
        assertThat(output1).isEqualTo(input);

        ResultModel output2 = converter.convertToEntityAttribute(encrypted2);
        assertThat(output2).isEqualTo(input);

    }
}
