package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.repository.TenantRepository;
import fr.dossierfacile.api.pdfgenerator.util.parameterresolvers.TenantResolver;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static fr.dossierfacile.api.pdfgenerator.util.PdfAssert.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(TenantResolver.class)
class EmptyBOPdfDocumentTemplateTest {

    @Autowired
    private EmptyBOPdfDocumentTemplate emptyBOPdfDocumentTemplate;

    @MockBean
    private TenantRepository tenantRepository;

    private final Path outputPdf = Paths.get("outputFile.pdf");
    private final Path expectedPdf = Paths.get("src/test/resources/expected/tax-custom-text.pdf");

    @Test
    void should_generate_pdf(Tenant tenant) throws IOException {
        Document document = Document.builder()
                .id(1L)
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.OTHER_TAX)
                .customText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. \uD83D\uDE01")
                .build();
        when(tenantRepository.getTenantByDocumentId(1L)).thenReturn(Optional.of(tenant));

        try (InputStream inputStream = emptyBOPdfDocumentTemplate.render(document)) {
            Files.copy(inputStream, outputPdf, StandardCopyOption.REPLACE_EXISTING);
            IOUtils.closeQuietly(inputStream);
        }

        assertThat(outputPdf)
                .containsText("""
                        Dr Who nous a indiqué ne pas pouvoir fournir d'avis d'imposition en son nom pour la raison suivante:
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore
                        et dolore magna aliqua. 😁
                        """)
                .isEqualTo(expectedPdf);
    }

    @AfterEach
    void tearDown() throws IOException {
        File file = outputPdf.toFile();
        if (file.exists()) {
            FileUtils.delete(file);
        }
    }

}