package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DossierPdfFormatTest {

    @Test
    void should_elide_names() {
        assertThat(DossierPdfFormat.of("Anne-Laure")).isEqualTo("d'Anne-Laure");
        assertThat(DossierPdfFormat.of("Élodie")).isEqualTo("d'Élodie");
        assertThat(DossierPdfFormat.of("Yves")).isEqualTo("d'Yves");
        assertThat(DossierPdfFormat.of("Yann")).isEqualTo("de Yann");
        assertThat(DossierPdfFormat.of("Hugo")).isEqualTo("de Hugo");
        assertThat(DossierPdfFormat.of("Camille")).isEqualTo("de Camille");
    }
}
