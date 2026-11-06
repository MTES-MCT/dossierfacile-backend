package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.repository.GuarantorRepository;
import fr.dossierfacile.api.pdfgenerator.repository.TenantRepository;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static fr.dossierfacile.api.pdfgenerator.service.templates.AttestationPdfTemplateTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmptyBOPdfDocumentTemplateTest {

    private static final long DOCUMENT_ID = 1L;
    private static final String CERTIFY_TENANT = "Attestation sur l’honneur Je soussigné·e Dr Who, atteste sur l’honneur que : ";
    private static final String SIGNED_BY_TENANT = " Fait le " + TODAY + " Signature Dr Who";
    private static final String CERTIFY_GUARANTOR = "Attestation sur l’honneur Je soussigné·e Jane Doe, atteste sur l’honneur que : ";
    private static final String SIGNED_BY_GUARANTOR = " Fait le " + TODAY + " Signature Jane Doe";

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private GuarantorRepository guarantorRepository;

    private EmptyBOPdfDocumentTemplate template;

    @BeforeEach
    void setUp() {
        template = new EmptyBOPdfDocumentTemplate(AttestationPdfTemplateTest.messageSource(), tenantRepository, guarantorRepository,
                AttestationPdfTemplateTest.template());
    }

    @Test
    void tax_attached_to_parents() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.MY_PARENTS).build();

        assertThat(render("tax-parents.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je suis rattaché·e au foyer fiscal de mes parents et ne dispose pas d’un avis à mon nom propre.\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void tax_without_first_declaration() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX)
                .documentCategoryStep(DocumentCategoryStep.TAX_NO_DECLARATION).build();

        assertThat(render("tax-no-declaration.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir un avis d’imposition car je n’ai pas encore effectué ma première déclaration.\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void tax_notice_not_received() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX)
                .documentCategoryStep(DocumentCategoryStep.TAX_NOT_RECEIVED).build();

        assertThat(render("tax-not-received.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir un avis d’imposition car je n’ai pas encore reçu mon avis "
                + "(ex. : première déclaration récente, traitement en cours).\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void tax_other_situation_prints_the_custom_text_as_an_explanation() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.OTHER_TAX)
                .customText("Je suis arrivée en France\nen septembre 2026.").build();

        assertThat(render("tax-other.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir un avis d’imposition.\" "
                + "Précisions : Je suis arrivée en France en septembre 2026."
                + SIGNED_BY_TENANT);
    }

    @Test
    void tax_other_situation_without_custom_text() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.LESS_THAN_YEAR).build();

        assertThat(render("tax-other-without-text.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir un avis d’imposition.\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void tax_of_a_guarantor_is_in_the_name_of_the_guarantor() throws Exception {
        ownedByGuarantor();
        Document document = document(DocumentCategory.TAX, DocumentSubCategory.MY_PARENTS).build();

        assertThat(render("tax-guarantor.pdf", document)).isEqualTo(CERTIFY_GUARANTOR
                + "\"Je suis rattaché·e au foyer fiscal de mes parents et ne dispose pas d’un avis à mon nom propre.\""
                + SIGNED_BY_GUARANTOR);
    }

    @Test
    void no_income_with_an_explanation() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.FINANCIAL, DocumentSubCategory.NO_INCOME)
                .customText("Je suis étudiante à temps plein, sans emploi ni bourse.").build();

        assertThat(render("financial-no-income.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne perçois pas de revenus.\" "
                + "Précisions : Je suis étudiante à temps plein, sans emploi ni bourse."
                + SIGNED_BY_TENANT);
    }

    @Test
    void no_income_without_explanation() throws Exception {
        ownedByTenant();
        // The tenant front sends "-" when the explanation is left empty
        Document document = document(DocumentCategory.FINANCIAL, DocumentSubCategory.NO_INCOME).customText("-").build();

        assertThat(render("financial-no-income-without-explanation.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne perçois pas de revenus.\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void other_financial_document_prints_the_custom_text_as_an_explanation() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.FINANCIAL, DocumentSubCategory.SALARY)
                .customText("Mon employeur ne m'a pas encore remis de bulletin de salaire.").build();

        assertThat(render("financial-other.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir ce justificatif.\" "
                + "Précisions : Mon employeur ne m'a pas encore remis de bulletin de salaire."
                + SIGNED_BY_TENANT);
    }

    @Test
    void other_residency_states_the_front_sentence_in_the_first_person() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.RESIDENCY, DocumentSubCategory.OTHER_RESIDENCY)
                .customText(EmptyBOPdfDocumentTemplate.FRONT_OTHER_RESIDENCY_TEXT).build();

        assertThat(render("residency-other.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir de quittance de logement pour ces 3 derniers mois.\""
                + SIGNED_BY_TENANT);
    }

    @Test
    void other_residency_prints_another_custom_text_as_an_explanation() throws Exception {
        ownedByTenant();
        Document document = document(DocumentCategory.RESIDENCY, DocumentSubCategory.OTHER_RESIDENCY)
                .customText("Je vis sur un bateau depuis deux ans.").build();

        assertThat(render("residency-other-custom-text.pdf", document)).isEqualTo(CERTIFY_TENANT
                + "\"Je ne suis pas en mesure de fournir de justificatif de domicile.\" "
                + "Précisions : Je vis sur un bateau depuis deux ans."
                + SIGNED_BY_TENANT);
    }

    @Test
    void other_residency_of_a_guarantor_is_in_the_name_of_the_guarantor() throws Exception {
        ownedByGuarantor();
        Document document = document(DocumentCategory.RESIDENCY, DocumentSubCategory.OTHER_RESIDENCY)
                .customText("Mon garant vit sur un bateau.").build();

        assertThat(render("residency-other-guarantor.pdf", document)).isEqualTo(CERTIFY_GUARANTOR
                + "\"Je ne suis pas en mesure de fournir de justificatif de domicile.\" "
                + "Précisions : Mon garant vit sur un bateau."
                + SIGNED_BY_GUARANTOR);
    }

    @Test
    void no_income_of_a_guarantor_is_in_the_name_of_the_guarantor() throws Exception {
        ownedByGuarantor();
        Document document = document(DocumentCategory.FINANCIAL, DocumentSubCategory.NO_INCOME)
                .customText("Mon garant est sans emploi.").build();

        assertThat(render("financial-no-income-guarantor.pdf", document)).isEqualTo(CERTIFY_GUARANTOR
                + "\"Je ne perçois pas de revenus.\" "
                + "Précisions : Mon garant est sans emploi."
                + SIGNED_BY_GUARANTOR);
    }

    private void ownedByTenant() {
        Tenant tenant = Tenant.builder().build();
        tenant.setFirstName("Dr");
        tenant.setLastName("Who");
        when(tenantRepository.getTenantByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(tenant));
    }

    private void ownedByGuarantor() {
        Guarantor guarantor = Guarantor.builder().typeGuarantor(TypeGuarantor.NATURAL_PERSON).firstName("Jane").lastName("Doe").build();
        when(tenantRepository.getTenantByDocumentId(DOCUMENT_ID)).thenReturn(Optional.empty());
        when(guarantorRepository.getGuarantorByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(guarantor));
    }

    private static Document.DocumentBuilder document(DocumentCategory category, DocumentSubCategory subCategory) {
        return Document.builder().id(DOCUMENT_ID).documentCategory(category).documentSubCategory(subCategory).noDocument(true);
    }

    private String render(String fileName, Document document) throws Exception {
        byte[] pdf = AttestationPdfTemplateTest.save(fileName, template.render(document));
        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            return AttestationPdfTemplateTest.textOf(pdDocument);
        }
    }

}
