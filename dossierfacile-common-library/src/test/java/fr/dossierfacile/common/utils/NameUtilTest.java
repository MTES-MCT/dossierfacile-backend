package fr.dossierfacile.common.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NameUtilTest {

    @ParameterizedTest(name = "[{index}] sanitizeForComparison(''{0}'') should return ''{1}''")
    @CsvSource({
            // Cas avec accents
            "Dupont, DUPONT",
            "Dupönt, DUPONT",
            "Élise, ELISE",
            "François, FRANCOIS",
            "José, JOSE",
            "Gérard, GERARD",
            "Hélène, HELENE",
            "Jérôme, JEROME",
            "Zoë, ZOE",
            "Noël, NOEL",

            // Cas avec tirets et espaces
            "Jean-Pierre, JEANPIERRE",
            "Marie-Claire, MARIECLAIRE",
            "De La Cruz, DELACRUZ",
            "Van Der Berg, VANDERBERG",
            "Le Goff, LEGOFF",

            // Cas avec apostrophes
            "O'Brien, OBRIEN",
            "D'Angelo, DANGELO",

            // Cas avec caractères spéciaux
            "Smith-Jones, SMITHJONES",
            "Müller, MULLER",
            "Søren, SOREN",
            "Łukasz, LUKASZ",

            // Cas mixtes complexes
            "Jean-François D'été, JEANFRANCOISDETE",
            "Marie-Hélène De Saint-Exupéry, MARIEHELENEDESAINTEXUPERY",

            // Cas avec espaces multiples
            "John   Doe, JOHNDOE",

            // Cas edge
            "'', ''",
            "   , ''",
            "123, ''",
            "a, A",

            // Cas réels français
            "François-Xavier, FRANCOISXAVIER",
            "Anne-Sophie, ANNESOPHIE",
            "Bérénice, BERENICE",
            "Cécile, CECILE",
    })
    void testSanitizeForComparison(String input, String expected) {
        String result = NameUtil.sanitizeForComparison(input);
        assertThat(result).isEqualTo(expected);
    }

    private static java.util.stream.Stream<Arguments> provideNameMatchingTestCases() {
        return java.util.stream.Stream.of(
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean", "Pierre"), "Dupont"),
                        new TestDocumentIdentity(List.of("Jean", "Pierre"), "Dupont"),
                        true,
                        "Exact match - Nom et prénoms identiques"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("François"), "Dupont"),
                        new TestDocumentIdentity(List.of("Francois"), "Dupont"),
                        true,
                        "Match avec accent - François vs Francois"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean-Pierre"), "Martin"),
                        new TestDocumentIdentity(List.of("JeanPierre"), "Martin"),
                        true,
                        "Match avec tiret - Jean-Pierre vs JeanPierre"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean", "Pierre", "Paul"), "Dupont"),
                        new TestDocumentIdentity(List.of("Pierre"), "Dupont"),
                        true,
                        "Match avec un prénom commun parmi plusieurs"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Marie"), "Dupont", "Martin"),
                        new TestDocumentIdentity(List.of("Marie"), "Martin"),
                        true,
                        "Match avec nom d'usage - preferredName Martin matche avec lastName Martin"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Marie"), "Martin"),
                        new TestDocumentIdentity(List.of("Marie"), "Dupont", "Martin"),
                        true,
                        "Match avec nom d'usage extraction - lastName Martin matche avec preferredName Martin"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Anne", "Sophie"), "De La Cruz"),
                        new TestDocumentIdentity(List.of("Anne"), "DeLaCruz"),
                        true,
                        "Match avec nom composé - De La Cruz vs DeLaCruz"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Søren"), "Larsen"),
                        new TestDocumentIdentity(List.of("Soren"), "Larsen"),
                        true,
                        "Match avec caractère scandinave - Søren vs Soren"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Łukasz"), "Kowalski"),
                        new TestDocumentIdentity(List.of("Lukasz"), "Kowalski"),
                        true,
                        "Match avec caractère slave - Łukasz vs Lukasz"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean"), "Dupont"),
                        new TestDocumentIdentity(List.of("Pierre"), "Dupont"),
                        false,
                        "Pas de match - Prénoms différents"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean"), "Dupont"),
                        new TestDocumentIdentity(List.of("Jean"), "Martin"),
                        false,
                        "Pas de match - Noms de famille différents"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean"), "Dupont"),
                        new TestDocumentIdentity(List.of("Pierre"), "Martin"),
                        false,
                        "Pas de match - Prénom et nom différents"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Marie"), "Dupont-Martin"),
                        new TestDocumentIdentity(List.of("Marie"), "Dupont"),
                        true,
                        "Match avec substring - Dupont-Martin contient Dupont"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("JEAN"), "DUPONT"),
                        new TestDocumentIdentity(List.of("jean"), "dupont"),
                        true,
                        "Match insensible à la casse"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Jean   Pierre"), "Du  Pont"),
                        new TestDocumentIdentity(List.of("JeanPierre"), "DuPont"),
                        true,
                        "Match avec espaces multiples ignorés"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Marie", "Hélène"), "Dupont", "De Saint-Exupéry"),
                        new TestDocumentIdentity(List.of("Marie"), "DeSaintExupery"),
                        true,
                        "Match avec nom d'usage composé et accents"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG"),
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG-TOURNESAC"),
                        true,
                        "Match Romaric 1"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG-TOURNESAC"),
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG"),
                        true,
                        "Match Romaric 2"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Romaric"), "TEST", "HALDENWANG-TOURNESAC"),
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG"),
                        true,
                        "Match Romaric 3"
                ),
                Arguments.of(
                        new TestDocumentIdentity(List.of("Romaric"), "TEST"),
                        new TestDocumentIdentity(List.of("Romaric"), "HALDENWANG", "TEST"),
                        true,
                        "Match Romaric 4"
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @org.junit.jupiter.params.provider.MethodSource("provideNameMatchingTestCases")
    void testIsNameMatching(TestDocumentIdentity document, TestDocumentIdentity extracted, boolean expectedMatch, String description) {
        boolean result = NameUtil.isNameMatching(document, extracted);
        assertThat(result)
                .as(description)
                .isEqualTo(expectedMatch);
    }

}
