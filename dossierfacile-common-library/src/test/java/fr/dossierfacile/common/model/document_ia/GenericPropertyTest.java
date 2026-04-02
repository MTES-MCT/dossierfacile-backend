package fr.dossierfacile.common.model.document_ia;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericPropertyTest {

    @Test
    void should_map_object_value_from_strict_map_schema() {
        GenericProperty beneficiaire = GenericProperty.builder()
                .name("beneficiaire")
                .type(GenericProperty.TYPE_OBJECT)
                .value(List.of(
                        nested("ligne1", "MR HALDENWANG TOURNESAC ROMARIC", "string"),
                        nested("qualite", null, "string"),
                        nested("prenom", null, "string"),
                        nested("nom", null, "string")
                ))
                .build();

        List<GenericProperty> mapped = beneficiaire.getObjectValue();

        assertThat(mapped).hasSize(4);
        assertThat(mapped).extracting(GenericProperty::getName)
                .containsExactly("ligne1", "qualite", "prenom", "nom");
        assertThat(mapped).extracting(GenericProperty::getType)
                .containsOnly("string");
    }

    @Test
    void should_reject_non_strict_object_schema() {
        GenericProperty beneficiaire = GenericProperty.builder()
                .name("beneficiaire")
                .type(GenericProperty.TYPE_OBJECT)
                .value(List.of(
                        Map.of("name", "ligne1", "value", "X", "type", "string", "extra", "forbidden")
                ))
                .build();

        assertThatThrownBy(beneficiaire::getObjectValue)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected keys: [name, value, type]");
    }

    private Map<String, Object> nested(String name, Object value, String type) {
        Map<String, Object> nested = new HashMap<>();
        nested.put("name", name);
        nested.put("value", value);
        nested.put("type", type);
        return nested;
    }
}
