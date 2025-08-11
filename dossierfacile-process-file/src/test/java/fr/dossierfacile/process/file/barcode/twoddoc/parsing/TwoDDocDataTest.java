package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_0A;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_41;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_43;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_44;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_45;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_46;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_47;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_4A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TwoDDocDataTest {

    @Test
    void should_parse_2ddoc_data() {
        TwoDDocData data = TwoDDocData.parse("431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D");

        assertAll(
                () -> assertThat(data.get(ID_0A)).isNull(),
                () -> assertThat(data.get(ID_41)).isEqualTo("42000"),
                () -> assertThat(data.get(ID_43)).isEqualTo("1,5"),
                () -> assertThat(data.get(ID_44)).isEqualTo("4237A18611332"),
                () -> assertThat(data.get(ID_45)).isEqualTo("2021"),
                () -> assertThat(data.get(ID_46)).isEqualTo("DOE JOHN"),
                () -> assertThat(data.get(ID_47)).isEqualTo("1267027554499"),
                () -> assertThat(data.get(ID_4A)).isEqualTo("31042022")
        );
    }

    @Test
    void should_format_2ddoc_data() {
        TwoDDocData data = TwoDDocData.parse("431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D");

        assertThat(data.withLabels()).containsAllEntriesOf(Map.of(
                "Année des revenus", "2021",
                "Date de mise en recouvrement", "31042022",
                "Déclarant 1", "DOE JOHN",
                "Nombre de parts", "1,5",
                "Numéro fiscal du déclarant 1", "1267027554499",
                "Revenu fiscal de référence", "42000",
                "Référence d’avis d’impôt", "4237A18611332"
        ));
    }

    @Test
    void should_parse_2025_avis_imposition_2ddoc_data_with_1_declarant() {
        TwoDDocData data = TwoDDocData.parse("431\u001D442544A3295123445202446MARIO BROSSE\u001D4A310720254YAVENUE DE LA REPUBLIQUE/75003 PARIS\u001D4712345404911074117311\u001D4V41\u001D4X126\u001D");

        assertThat(data.withLabels()).containsAllEntriesOf(Map.ofEntries(
            Map.entry("Année des revenus", "2024"),
            Map.entry("Date de mise en recouvrement", "31072025"),
            Map.entry("Déclarant 1", "MARIO BROSSE"),
            Map.entry("Nombre de parts", "1"),
            Map.entry("Numéro fiscal du déclarant 1", "1234540491107"),
            Map.entry("Revenu fiscal de référence", "17311"),
            Map.entry("Impot sur le revenu net", "41"),
            Map.entry("Retenue à la source", "126"),
            Map.entry("Référence d’avis d’impôt", "2544A32951234"),
            Map.entry("Adresse", "AVENUE DE LA REPUBLIQUE/75003 PARIS")
        ));
    }

    @Test
    void should_parse_2025_avis_imposition_2ddoc_data_with_2_declarant() {
        TwoDDocData data = TwoDDocData.parse("432\u001D44256901234879045202446TIMOTHEE QUENTIN\u001D4A310720254Y1 ESPLANADE DE LA DEFENSE/PUTEAUX\u001D47301284563329648MARIE DUPONT\u001D49301791234511341100009\u001D4V18488\u001D4X16873\u001D");

        assertThat(data.withLabels()).containsAllEntriesOf(Map.ofEntries(
                Map.entry("Année des revenus", "2024"),
                Map.entry("Date de mise en recouvrement", "31072025"),
                Map.entry("Déclarant 1", "TIMOTHEE QUENTIN"),
                Map.entry("Déclarant 2", "MARIE DUPONT"),
                Map.entry("Nombre de parts", "2"),
                Map.entry("Numéro fiscal du déclarant 1", "3012845633296"),
                Map.entry("Numéro fiscal du déclarant 2", "3017912345113"),
                Map.entry("Revenu fiscal de référence", "100009"),
                Map.entry("Impot sur le revenu net", "18488"),
                Map.entry("Retenue à la source", "16873"),
                Map.entry("Référence d’avis d’impôt", "2569012348790"),
                Map.entry("Adresse", "1 ESPLANADE DE LA DEFENSE/PUTEAUX")
        ));
    }
}