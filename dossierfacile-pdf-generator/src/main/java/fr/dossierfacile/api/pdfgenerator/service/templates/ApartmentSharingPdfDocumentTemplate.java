package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.pdfgenerator.exception.TenantNotFoundException;
import fr.dossierfacile.api.pdfgenerator.model.TargetImageData;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.api.pdfgenerator.util.PdfOptimizer;
import fr.dossierfacile.api.pdfgenerator.util.Utility;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.Matrix;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingPdfDocumentTemplate implements PdfTemplate<ApartmentSharing> {

    private static final Resource dgfipIcon = new ClassPathResource("dgfip-icon.png");
    private static final Resource verifiedIcon = new ClassPathResource("verified.png");

    //region Important dimensions in template (static/pdf/template_Dossier_PDF_attachments_and_clarification.pdf)
    private static final float A_WIDTH_TEMPLATE = 2479f; //(original : 297.5px)
    private static final float B_HEIGHT_TEMPLATE = 3508f; //(original : 421px)
    private static final float C_WIDTH_AVAILABLE_AREA_TEMPLATE = A_WIDTH_TEMPLATE / 297.5f * 265.5f;
    private static final float D_HEIGHT_AVAILABLE_AREA_TEMPLATE = B_HEIGHT_TEMPLATE / 421 * 302.5f;
    private static final float X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE = A_WIDTH_TEMPLATE / 297.5f * 16;
    private static final float Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE = B_HEIGHT_TEMPLATE / 421 * 92.5f;
    //endregion

    //region Sentences by category documents in the header of Attachments template
    private static final String BEGIN_OF_TEXT_HEADER = "Le dossier de ";
    private static final String TENANT_IDENTIFICATION_TEXT_HEADER = "La pièce d'identité de ";
    private static final String TENANT_RESIDENCY_TEXT_HEADER = "Le justificatif de domicile de ";
    private static final String TENANT_PROFESSIONAL_TEXT_HEADER = "Le justificatif de situation professionnelle de ";
    private static final String TENANT_FINANCIAL_TEXT_HEADER = "Le justificatif de ressources n°";
    private static final String TENANT_TAX_TEXT_HEADER = "L'avis d'imposition conforme de ";
    private static final String NATURAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "La pièce d'identité du garant ";
    private static final String NATURAL_GUARANTOR_RESIDENCY_TEXT_HEADER = "Le justificatif de domicile du garant ";
    private static final String NATURAL_GUARANTOR_PROFESSIONAL_TEXT_HEADER = "Le justificatif de situation professionnelle du garant ";
    private static final String NATURAL_GUARANTOR_FINANCIAL_TEXT_HEADER = "Le justificatif de ressources n°";
    private static final String NATURAL_GUARANTOR_TAX_TEXT_HEADER = "L'avis d'imposition conforme du garant ";
    private static final String LEGAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "Identité de la personne morale qui se porte garant pour ";
    private static final String LEGAL_GUARANTOR_LEGAL_IDENTIFICATION_TEXT_HEADER = "Justificatif d'identité du représentant de la personne morale qui se porte garant pour ";
    private static final String ORGANISM_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "L'attestation de l'organisme qui se porte garant pour ";
    //endregion

    //region Static sentences
    private static final String LE_MOT_DU_LOCATAIRE = "Le mot du locataire";
    private static final String LE_DOSSIER_EN_UN_CLIN_D_OEIL = "Le dossier en un clin d’oeil";
    private static final String TYPE_DE_DOSSIER = "Type de dossier";
    private static final String REVENUS_MENSUELS_NETS_CUMULES = "Revenus mensuels nets cumulés";
    private static final String LEUR_GARANT = "Leur(s) garant(s)";
    private static final String LES_PIECES_JUSTIFICATIVES = "Les pièces justificatives de";
    private static final String LES_PIECES_JUSTIFICATIVES_DE_SON_GARANT = "Les pièces justificatives de son garant";
    private static final String PAS_DE_GARANTS = "Pas de garants";
    private static final String GARANTS_PHYSIQUES = " garants physiques";
    private static final String GARANT_PHYSIQUE = " garant physique";
    private static final String ORGANISMES_GARANTS = " organismes garants";
    private static final String ORGANISME_GARANT = " organisme garant";
    private static final String GARANTS_MORAUX = " garants moraux";
    private static final String GARANT_MORAL = " garant moral";
    private static final String TITLE_FOR_GUARANTOR_LEGAL_PERSON = "Garant personne morale";
    private static final String TITLE_FOR_GUARANTOR_ORGANISM = "Organisme garant";
    //endregion

    //region Sentences for Type of Flatsharing
    private static final String DOSSIER_SEUL = "Dossier seul";
    private static final String DOSSIER_EN_COUPLE = "Dossier en couple";
    private static final String DOSSIER_EN_COLOCATION = "Dossier en colocation";
    //endregion

    //region Loading FONTS
    private static final Resource FONT_MARIANNE_LIGHT = new ClassPathResource("static/fonts/Marianne-Light.ttf");
    private static final Resource FONT_MARIANNE_REGULAR = new ClassPathResource("static/fonts/Marianne-Regular.ttf");
    private static final Resource FONT_MARIANNE_BOLD = new ClassPathResource("static/fonts/Marianne-Bold.ttf");

    private static final Resource FONT_SPECTRAL_BOLD = new ClassPathResource("static/fonts/Spectral-Bold.ttf");
    private static final Resource FONT_SPECTRAL_EXTRA_BOLD = new ClassPathResource("static/fonts/Spectral-ExtraBold.ttf");
    private static final Resource FONT_SPECTRAL_ITALIC = new ClassPathResource("static/fonts/Spectral-Italic.ttf");

    //endregion
    //region First templates
    private static final Resource TEMPLATE_OF_FIRST_INDEXPAGES = new ClassPathResource("static/pdf/template_Dossier_PDF_first_page_1.pdf");
    private static final Resource TEMPLATE_OF_OTHER_INDEXPAGES = new ClassPathResource("static/pdf/template_Dossier_PDF_first_page_2.pdf");
    private static final Resource TEMPLATE_OF_ATTACHMENTS_AND_CLARIFICATIONS = new ClassPathResource("static/pdf/template_Dossier_PDF_attachments_and_clarification.pdf");
    //endregion
    //region Other dimensions
    private static final float LEFT_MARGIN_FOR_LEFT_TENANT = A_WIDTH_TEMPLATE / 297.5f * 16;
    private static final float LEFT_MARGIN_FOR_RIGHT_TENANT = A_WIDTH_TEMPLATE / 297.5f * 150;
    private static final float RIGHT_MARGIN_FOR_TEXT_INCOMING = A_WIDTH_TEMPLATE / 297.5f * 8;
    //--------------------------------------------------------------------------------------------------
    //First page of indexes
    private static final float Y_LOCATION_STATIC_TEXT_IN_FIRST_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 283f;
    private static final float Y_LOCATION_OF_TITLE_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 230.99f;
    private static final float Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 224;
    private static final float Y_LOCATION_OF_EMAIL_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 219.5f;
    private static final float Y_LOCATION_OF_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 212.99f;
    //--------------------------------------------------------------------------------------------------
    //Second page of indexes
    private static final float Y_LOCATION_OF_TITLE_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 312.5f;
    private static final float Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 305.5f;
    private static final float Y_LOCATION_OF_EMAIL_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 301;
    private static final float Y_LOCATION_OF_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE = B_HEIGHT_TEMPLATE / 421 * 294.5f;
    //--------------------------------------------------------------------------------------------------
    //Common dimensions
    private static final float SEPARATION_BETWEEN_GROUPS_OF_DOCUMENT_INDEXES = B_HEIGHT_TEMPLATE / 421 * 13.5f;
    private static final float SEPARATION_BETWEEN_TITLE_AND_NAME_OF_SUBJECT_IN_GROUP_OF_DOCUMENT_INDEXES = Y_LOCATION_OF_TITLE_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE - Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
    private static final float SEPARATION_BETWEEN_NAME_OF_SUBJECT_AND_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES = Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE - Y_LOCATION_OF_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
    private static final float X_LOCATION_OF_END_OF_LEFT_RECTANGULE_IN_INDEXPAGES = A_WIDTH_TEMPLATE / 297.5f * 148;
    private static final float X_LOCATION_OF_END_OF_RIGHT_RECTANGULE_IN_INDEXPAGES = A_WIDTH_TEMPLATE / 297.5f * 282;

    private static final float RIGHT_MARGIN_FOR_PAGINATION = A_WIDTH_TEMPLATE / 297.5f * 21.60f;
    private static final float BOTTOM_MARGIN_FOR_PAGINATION = B_HEIGHT_TEMPLATE / 421 * 9.60f;
    private static final float FONT_SIZE_FOR_PAGINATION = B_HEIGHT_TEMPLATE / 421 * 7.2f;

    private static final float LEFT_MARGIN_FOR_BEGIN_OF_TEXT_HEADER = A_WIDTH_TEMPLATE / 297.5f * 24;
    private static final float FONT_SIZE_FOR_BEGIN_OF_TEXT_HEADER = B_HEIGHT_TEMPLATE / 421 * 6;
    private static final float Y_LOCATION_OF_BEGIN_OF_TEXT_HEADER = B_HEIGHT_TEMPLATE / 421 * 355.1f;

    private static final float LEFT_MARGIN_FOR_NAME_OF_TENANTS = A_WIDTH_TEMPLATE / 297.5f * 64.5f;
    private static final float FONT_SIZE_FOR_NAME_OF_TENANTS = B_HEIGHT_TEMPLATE / 421 * 6.28f;
    private static final float Y_LOCATION_OF_NAME_OF_TENANTS = Y_LOCATION_OF_BEGIN_OF_TEXT_HEADER;

    private static final float LEFT_MARGIN_FOR_HEADER_SENTENCE = LEFT_MARGIN_FOR_BEGIN_OF_TEXT_HEADER;
    private static final float FONT_SIZE_FOR_HEADER_SENTENCE = FONT_SIZE_FOR_NAME_OF_TENANTS;
    private static final float Y_LOCATION_OF_HEADER_SENTENCE = B_HEIGHT_TEMPLATE / 421 * 343.6f;

    private static final float FONT_SIZE_FOR_CLARIFICATION_TEXT = B_HEIGHT_TEMPLATE / 421 * 4.88f;
    private static final float LEADING_FOR_CLARIFICATION_TEXT = B_HEIGHT_TEMPLATE / 421 * 8.23f;
    private static final float LEFT_MARGIN_FOR_CLARIFICATION_TEXT = FONT_SIZE_FOR_PAGINATION;

    private static final float FONT_SIZE_FOR_STATIC_TEXT_IN_FIRST_TEMPLATE = B_HEIGHT_TEMPLATE / 421 * 8.37f;

    private static final float FONT_SIZE_FOR_FIRST_NAMES_OF_TENANTS_IN_HEADER_OF_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 10f;
    private static final float Y_LOCATION_FOR_FIRST_NAMES_OF_TENANTS_IN_HEADER_OF_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 357.03f;

    private static final float FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 4.5f;
    private static final float FONT_SIZE_FOR_CONTENT_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 3.78f;
    private static final float Y_LOCATION_OF_CONTENT_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 251.899f;

    private static final float FONT_SIZE_FOR_TITLE_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES = FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
    private static final float FONT_SIZE_FOR_CONTENT_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 6.48f;
    private static final float Y_LOCATION_OF_CONTENT_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 250.999f;

    private static final float FONT_SIZE_FOR_TITLE_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
    private static final float FONT_SIZE_FOR_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = FONT_SIZE_FOR_CONTENT_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
    private static final float LEADING_FOR_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 6f;
    private static final float FONT_SIZE_FOR_SMALL_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 3.42f;
    private static final float LEADING_FOR_SMALL_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = B_HEIGHT_TEMPLATE / 421 * 4.2f;
    private static final float X_LOCATION_OF_BEGIN_OF_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = A_WIDTH_TEMPLATE / 297.5f * 214.81f;
    private static final float Y_LOCATION_OF_BEGIN_OF_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES = Y_LOCATION_OF_CONTENT_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES;

    private static final float X_LOCATION_BEGIN_OF_FIRST_RECTANGULE = A_WIDTH_TEMPLATE / 297.5f * 16;
    private static final float X_LOCATION_BEGIN_OF_THIRD_RECTANGULE = A_WIDTH_TEMPLATE / 297.5f * 196;

    private static final float WIDTH_OF_ALL_THREE_RECTANGULES = A_WIDTH_TEMPLATE / 297.5f * 86;
    private static final float Y_LOCATION_OF_TITLE_OF_ALL_THREE_RECTANGULES = B_HEIGHT_TEMPLATE / 421 * 265.225f;

    private static final float FONT_SIZE_FOR_TITLE_OF_GROUP_OF_INDEXES = FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;

    private static final float WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES = A_WIDTH_TEMPLATE / 297.5f * 132;

    private static final float FONT_SIZE_FOR_CONTENT_OF_GROUP_OF_INDEXES = B_HEIGHT_TEMPLATE / 421 * 3.6f;

    private static final float FONT_SIZE_SMALL = 22;
    private static final float ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES = A_WIDTH_TEMPLATE / 297.5f * 6;
    private static final float LEADING_FOR_CONTENT_OF_GROUP_OF_INDEXES = B_HEIGHT_TEMPLATE / 421 * 7.08f;

    private static final float DGFIP_ICON_WIDTH = 60;

    private static final float FONT_SIZE_FOR_INCOMING_NEXT_TO_INDEX_OF_FINANCIAL_DOCUMENTS = FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
    //endregion

    //region Bookmark's titles
    private static final String TITLE_0_DOSSIER_PDF = "Dossier PDF";
    private static final String TITLE_1_INDEX = "Index";
    private static final String TITLE_2_CLARIFICATION = "Clarification";
    private static final String TITLE_3_TENANT = "Tenant";
    private static final String TITLE_3_1_GUARANTOR = "Guarantor";
    //endregion

    private static final Color DARK_GREEN = new Color(70, 105, 100);
    private static final Color GREEN = new Color(0, 172, 140);

    private static final Color LIGHT_GREEN = new Color(223, 253, 247);
    private static final Color DARK_GRAY = new Color(30, 30, 30);
    private static final Color GRAY = new Color(56, 56, 56);
    private static final Color LIGHT_GRAY = new Color(106, 106, 106);

    private final Locale locale = LocaleContextHolder.getLocale();
    private final TenantCommonRepository tenantRepository;
    private final DownloadService downloadDocumentService;
    private final MessageSource messageSource;

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private void addPaginate(PDDocument doc) throws IOException {

        PDFont font = PDType1Font.HELVETICA_BOLD;
        int numberPage = 1;
        int totalPages = doc.getNumberOfPages();
        for (PDPage page : doc.getPages()) {
            PDRectangle pageSize = page.getMediaBox();
            float pageWidth = pageSize.getWidth();
            float centerX = pageWidth - RIGHT_MARGIN_FOR_PAGINATION;
            float centerY = BOTTOM_MARGIN_FOR_PAGINATION;

            // append the content to the existing stream
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.beginText();
                contentStream.setNonStrokingColor(Color.DARK_GRAY);
                contentStream.setFont(font, FONT_SIZE_FOR_PAGINATION / 1.2f);
                contentStream.setTextMatrix(Matrix.getTranslateInstance(centerX, centerY));
                contentStream.showText(numberPage++ + "/" + totalPages);
                contentStream.endText();
            } catch (IOException e) {
                log.error("Error adding paginate to create full pdf");
                log.error(e.getMessage(), e.getCause());
            }
        }
    }

    private ByteArrayOutputStream addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(List<Tenant> tenantList, String headerSentence, String bodyText) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument doc = PDDocument.load(TEMPLATE_OF_ATTACHMENTS_AND_CLARIFICATIONS.getInputStream())) {

            //region Reading fonts
            PDType0Font font1 = PDType0Font.load(doc, FONT_MARIANNE_LIGHT.getInputStream());

            PDType0Font font2 = PDType0Font.load(doc, FONT_SPECTRAL_BOLD.getInputStream());

            PDType0Font font3 = PDType0Font.load(doc, FONT_SPECTRAL_EXTRA_BOLD.getInputStream());

            PDType0Font font4 = PDType0Font.load(doc, FONT_SPECTRAL_ITALIC.getInputStream());
            //endregion

            PDPage pageTemplate = doc.getPage(0);

            //region Text Header 1
            PDPageContentStream contentStream1 = new PDPageContentStream(doc, pageTemplate, PDPageContentStream.AppendMode.APPEND, true);
            contentStream1.beginText();
            contentStream1.setFont(font1, FONT_SIZE_FOR_BEGIN_OF_TEXT_HEADER);
            contentStream1.setNonStrokingColor(GREEN);
            contentStream1.newLineAtOffset(LEFT_MARGIN_FOR_BEGIN_OF_TEXT_HEADER, Y_LOCATION_OF_BEGIN_OF_TEXT_HEADER);
            contentStream1.showText(BEGIN_OF_TEXT_HEADER);
            contentStream1.endText();
            contentStream1.close();
            //endregion

            //region Text Header 2
            PDPageContentStream contentStream2 = new PDPageContentStream(doc, pageTemplate, PDPageContentStream.AppendMode.APPEND, true);
            contentStream2.beginText();
            contentStream2.setFont(font2, FONT_SIZE_FOR_NAME_OF_TENANTS);
            contentStream2.setNonStrokingColor(GREEN);
            contentStream2.newLineAtOffset(LEFT_MARGIN_FOR_NAME_OF_TENANTS, Y_LOCATION_OF_NAME_OF_TENANTS);
            contentStream2.showText(concatenateTheFullTenantNames(tenantList));
            contentStream2.endText();
            contentStream2.close();
            //endregion

            //region Text Header 3
            PDPageContentStream contentStream3 = new PDPageContentStream(doc, pageTemplate, PDPageContentStream.AppendMode.APPEND, true);
            contentStream3.beginText();
            contentStream3.setFont(font3, FONT_SIZE_FOR_HEADER_SENTENCE);
            contentStream3.setNonStrokingColor(GRAY);
            contentStream3.newLineAtOffset(LEFT_MARGIN_FOR_HEADER_SENTENCE, Y_LOCATION_OF_HEADER_SENTENCE);
            contentStream3.showText(headerSentence);
            contentStream3.endText();
            contentStream3.close();
            //endregion

            //region Text Body
            if (bodyText != null && !bodyText.isBlank()) {
                PDPageContentStream contentStream4 = new PDPageContentStream(doc, pageTemplate, PDPageContentStream.AppendMode.APPEND, true);
                contentStream4.beginText();
                contentStream4.setNonStrokingColor(GRAY);
                contentStream4.setLeading(LEADING_FOR_CLARIFICATION_TEXT);
                contentStream4.newLine();
                contentStream4.newLine();
                contentStream4.newLine();
                contentStream4.newLine();

                bodyText = StringUtils.normalizeSpace(bodyText);

                float marginX = LEFT_MARGIN_FOR_CLARIFICATION_TEXT;
                Utility.addParagraph(
                        contentStream4,
                        C_WIDTH_AVAILABLE_AREA_TEMPLATE - 2 * marginX,
                        X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE + marginX,
                        B_HEIGHT_TEMPLATE - Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE,
                        Collections.singletonList(bodyText),
                        true,
                        font4,
                        FONT_SIZE_FOR_CLARIFICATION_TEXT
                );
                contentStream4.endText();
                contentStream4.close();
            }
            //endregion

            doc.save(outputStream);

        } catch (IOException e) {
            log.error("Problem when adding content to pdf template of attachments and clarification");
            log.error(e.getMessage(), e.getCause());
        }
        return outputStream;
    }

    private String concatenateTheFullTenantNames(List<Tenant> tenantList) {
        StringBuilder concatenatedNames = new StringBuilder();
        int lastIndex = tenantList.size() - 1;
        int currentIndex = 0;
        for (Tenant tenant : tenantList) {
            if (currentIndex == lastIndex) {
                concatenatedNames.append(tenant.getFullName());
            } else if (currentIndex == lastIndex - 1) {
                concatenatedNames.append(tenant.getFullName()).append(" et ");
            } else {
                concatenatedNames.append(tenant.getFullName()).append(", ");
            }
            currentIndex++;
        }
        return concatenatedNames.toString();
    }

    private String getSentenceForTenantFromDocumentCategory(Tenant tenant, DocumentCategory documentCategory, int count) {
        switch (documentCategory) {
            case IDENTIFICATION -> {
                return TENANT_IDENTIFICATION_TEXT_HEADER + tenant.getFirstName();
            }
            case RESIDENCY -> {
                return TENANT_RESIDENCY_TEXT_HEADER + tenant.getFirstName();
            }
            case PROFESSIONAL -> {
                return TENANT_PROFESSIONAL_TEXT_HEADER + tenant.getFirstName();
            }
            case FINANCIAL -> {
                return TENANT_FINANCIAL_TEXT_HEADER + count + " de " + tenant.getFirstName();
            }
            case TAX -> {
                return TENANT_TAX_TEXT_HEADER + tenant.getFirstName();
            }
            default -> {
                return "UNDEFINED ";
            }
        }
    }

    private String getSentenceForGuarantorFromDocumentCategory(int numberOfGuarantor, TypeGuarantor typeGuarantor, DocumentCategory documentCategory, String tenantFirstName, int count) {
        switch (typeGuarantor) {
            case NATURAL_PERSON -> {
                String followingPartOfSentence = (numberOfGuarantor > 0 ? "n°" + numberOfGuarantor : "") + " de " + tenantFirstName;

                switch (documentCategory) {
                    case IDENTIFICATION -> {
                        return NATURAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER + followingPartOfSentence;
                    }
                    case RESIDENCY -> {
                        return NATURAL_GUARANTOR_RESIDENCY_TEXT_HEADER + followingPartOfSentence;
                    }
                    case PROFESSIONAL -> {
                        return NATURAL_GUARANTOR_PROFESSIONAL_TEXT_HEADER + followingPartOfSentence;
                    }
                    case FINANCIAL -> {
                        return NATURAL_GUARANTOR_FINANCIAL_TEXT_HEADER + count + " du garant " + followingPartOfSentence;
                    }
                    case TAX -> {
                        return NATURAL_GUARANTOR_TAX_TEXT_HEADER + followingPartOfSentence;
                    }
                    default -> {
                        return "UNDEFINED ";
                    }
                }
            }
            case LEGAL_PERSON -> {
                switch (documentCategory) {
                    case IDENTIFICATION -> {
                        return LEGAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER + tenantFirstName;
                    }
                    case IDENTIFICATION_LEGAL_PERSON -> {
                        return LEGAL_GUARANTOR_LEGAL_IDENTIFICATION_TEXT_HEADER + tenantFirstName;
                    }
                    default -> {
                        return "UNDEFINED ";
                    }
                }
            }
            case ORGANISM -> {
                return ORGANISM_GUARANTOR_IDENTIFICATION_TEXT_HEADER + tenantFirstName;
            }
            default -> {
                return "UNDEFINED ";
            }
        }
    }

    private int calculateTotalIncomingOfGuarantor(Guarantor guarantor) {
        return Optional.ofNullable(guarantor.getDocuments())
                .orElse(new ArrayList<>())
                .stream().filter(document -> document.getDocumentCategory() == DocumentCategory.FINANCIAL).map(Document::getMonthlySum)
                .filter(Objects::nonNull).reduce(0, Integer::sum);
    }

    private String concatenateTheFirstTenantNames(List<Tenant> tenantList) {
        StringBuilder concatenatedNames = new StringBuilder();
        int lastIndex = tenantList.size() - 1;
        int currentIndex = 0;
        for (Tenant tenant : tenantList) {
            if (currentIndex == lastIndex) {
                concatenatedNames.append(tenant.getFirstName());
            } else if (currentIndex == lastIndex - 1) {
                concatenatedNames.append(tenant.getFirstName()).append(" et ");
            } else {
                concatenatedNames.append(tenant.getFirstName()).append(", ");
            }
            currentIndex++;
        }
        return concatenatedNames.toString();
    }

    private TargetImageData adjustSourceImageToAvailableAreaInTemplate(float sourceWidth, float sourceHeight) {
        float ratioSource = sourceHeight / sourceWidth;
        float ratioAvailableAreaInTemplate = D_HEIGHT_AVAILABLE_AREA_TEMPLATE / C_WIDTH_AVAILABLE_AREA_TEMPLATE;

        if (ratioSource <= ratioAvailableAreaInTemplate) {
            float targetWidth = C_WIDTH_AVAILABLE_AREA_TEMPLATE;
            float targetHeight = C_WIDTH_AVAILABLE_AREA_TEMPLATE * sourceHeight / sourceWidth;
            float horizontalDisplacement = X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE;
            float verticalDisplacement = B_HEIGHT_TEMPLATE - (Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE + D_HEIGHT_AVAILABLE_AREA_TEMPLATE) + (D_HEIGHT_AVAILABLE_AREA_TEMPLATE - targetHeight) / 2;
            return TargetImageData.builder()
                    .targetWidth(targetWidth)
                    .targetHeight(targetHeight)
                    .horizontalDisplacement(horizontalDisplacement)
                    .verticalDisplacement(verticalDisplacement)
                    .build();
        } else {
            float targetWidth = sourceWidth * D_HEIGHT_AVAILABLE_AREA_TEMPLATE / sourceHeight;
            float targetHeight = D_HEIGHT_AVAILABLE_AREA_TEMPLATE;
            float horizontalDisplacement = X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE + (C_WIDTH_AVAILABLE_AREA_TEMPLATE - targetWidth) / 2;
            float verticalDisplacement = B_HEIGHT_TEMPLATE - (Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE + D_HEIGHT_AVAILABLE_AREA_TEMPLATE);
            return TargetImageData.builder()
                    .targetWidth(targetWidth)
                    .targetHeight(targetHeight)
                    .horizontalDisplacement(horizontalDisplacement)
                    .verticalDisplacement(verticalDisplacement)
                    .build();
        }
    }

    private ByteArrayOutputStream mergePageInsideTemplate(PDDocument innerDocument, PDPage innerPage, byte[] templateBytes, String headerSentence) {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            TargetImageData targetImageData = adjustSourceImageToAvailableAreaInTemplate(innerPage.getMediaBox().getWidth(), innerPage.getMediaBox().getHeight());
            double widthScale = targetImageData.getTargetWidth() / innerPage.getMediaBox().getWidth();
            double heightScale = targetImageData.getTargetHeight() / innerPage.getMediaBox().getHeight();

            try (PDDocument document = PDDocument.load(templateBytes)) {

                LayerUtility layerUtility = new LayerUtility(document);
                PDFormXObject innerPageAsForm = layerUtility.importPageAsForm(innerDocument, innerPage);

                AffineTransform affineTransform = new AffineTransform();
                affineTransform.translate(targetImageData.getHorizontalDisplacement(), targetImageData.getVerticalDisplacement());
                affineTransform.scale(widthScale, heightScale);
                PDPage destPage = document.getPage(0);

                layerUtility.wrapInSaveRestore(destPage);
                layerUtility.appendFormAsLayer(destPage, innerPageAsForm, affineTransform, headerSentence);

                document.save(result);
            }

        } catch (IOException e) {
            log.error("Problem when printing attachment inside template of attachments");
            log.error(e.getMessage(), e.getCause());
        }
        return result;
    }

    private void createFirstsPages(PDFMergerUtility ut, int numberOfTenants, List<Integer> indexPagesForDocuments, PDOutlineItem pdOutlineItem) {
        try {
            int numberOfPagesAdded = (numberOfTenants + 1) / 2; // 2 tenants by page
            if (numberOfPagesAdded > 0) {
                ut.addSource(TEMPLATE_OF_FIRST_INDEXPAGES.getInputStream());
                for (int i = 1; i < numberOfPagesAdded; i++) {
                    ut.addSource(TEMPLATE_OF_OTHER_INDEXPAGES.getInputStream());
                }
            }
            log.info("Number of first pages added [" + numberOfPagesAdded + "]");
            indexPagesForDocuments.add(numberOfPagesAdded);

            //region Adding bookmark
            PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
            destination.setPageNumber(0);
            destination.setTop((int) B_HEIGHT_TEMPLATE);

            PDOutlineItem pdO = new PDOutlineItem();
            pdO.setTitle(TITLE_1_INDEX);
            pdO.setDestination(destination);

            pdOutlineItem.addLast(pdO);
            //endregion
        } catch (IOException e) {
            log.error("Problem creating first pages");
            log.error(e.getMessage(), e.getCause());
        }
    }

    private void addDocument(PDFMergerUtility ut, InputStream pdfDocument, List<Integer> indexPagesForDocuments, boolean newCategoryDocument, List<Tenant> tenantList, String headerSentence) {

        ByteArrayOutputStream templateWithTextsHeader = addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(tenantList, headerSentence, null);

        try (PDDocument innerDocument = PDDocument.load(pdfDocument)) {

            for (PDPage innerPage : innerDocument.getPages()) {
                ByteArrayOutputStream pdfDocPageWithAttachmentMerged = mergePageInsideTemplate(innerDocument, innerPage, templateWithTextsHeader.toByteArray(), headerSentence);
                ut.addSource(new ByteArrayInputStream(pdfDocPageWithAttachmentMerged.toByteArray()));
            }
            if (newCategoryDocument) {
                indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + innerDocument.getNumberOfPages());
            } else {
                indexPagesForDocuments.set(indexPagesForDocuments.size() - 1, indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + innerDocument.getNumberOfPages());
            }
        } catch (Exception e) {
            log.error("Unable to addDocument - headerSentence:" + headerSentence, e);
        }

    }

    private void addDocumentOfClarification(PDFMergerUtility ut, List<Tenant> tenantList, Tenant mainTenant, List<Integer> indexPagesForDocuments, PDOutlineItem pdOutlineItem) {
        if (StringUtils.isNotBlank(mainTenant.getClarification())) {
            //region Adding bookmark
            PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
            destination.setPageNumber(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1));
            destination.setTop((int) B_HEIGHT_TEMPLATE);

            PDOutlineItem pdO = new PDOutlineItem();
            pdO.setTitle(TITLE_2_CLARIFICATION);
            pdO.setDestination(destination);

            pdOutlineItem.addLast(pdO);
            //endregion

            ByteArrayOutputStream outputStream = addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(tenantList, LE_MOT_DU_LOCATAIRE, mainTenant.getClarification());
            ut.addSource(new ByteArrayInputStream(outputStream.toByteArray()));
            indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + 1);
        } else {
            indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1)); // does not exist - stay on current page
        }
    }

    private String getSentenceByApplicationType(ApplicationType applicationType) {
        switch (applicationType) {
            case ALONE -> {
                return DOSSIER_SEUL;
            }
            case COUPLE -> {
                return DOSSIER_EN_COUPLE;
            }
            case GROUP -> {
                return DOSSIER_EN_COLOCATION;
            }
        }
        return "";
    }

    private void addFirstNamesOfTenantsInTheHeaderOfCurrentIndexPage(int indexPage, PDDocument doc, List<Tenant> tenantList, PDType0Font font) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(GRAY);
        contentStream1.beginText();

        float fontSize = FONT_SIZE_FOR_FIRST_NAMES_OF_TENANTS_IN_HEADER_OF_INDEXPAGES;
        contentStream1.setFont(font, fontSize);
        String concatenatedNames = concatenateTheFirstTenantNames(tenantList);
        float textSize = fontSize * font.getStringWidth(concatenatedNames) / 1000;
        float offset = (A_WIDTH_TEMPLATE - textSize) / 2;
        contentStream1.newLineAtOffset(offset, Y_LOCATION_FOR_FIRST_NAMES_OF_TENANTS_IN_HEADER_OF_INDEXPAGES);
        contentStream1.showText(concatenatedNames);
        contentStream1.endText();
        contentStream1.close();
    }

    private void addStaticTextInFirstTemplateOfIndexes(int indexPage, PDDocument doc, PDType0Font font) throws IOException {
        PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream2.setNonStrokingColor(GREEN);
        contentStream2.beginText();
        float fontSize = FONT_SIZE_FOR_STATIC_TEXT_IN_FIRST_TEMPLATE;
        contentStream2.setFont(font, fontSize);
        float textSize = fontSize * font.getStringWidth(LE_DOSSIER_EN_UN_CLIN_D_OEIL) / 1000;
        float offset = (A_WIDTH_TEMPLATE - textSize) / 2;
        contentStream2.newLineAtOffset(offset, Y_LOCATION_STATIC_TEXT_IN_FIRST_INDEXPAGE);
        contentStream2.showText(LE_DOSSIER_EN_UN_CLIN_D_OEIL);
        contentStream2.endText();
        contentStream2.close();
    }

    private void addContentInFirstRectanguleInFirstTemplateOfIndexes(int indexPage, PDDocument doc, PDType0Font font, ApplicationType applicationType) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(LIGHT_GRAY);
        contentStream1.beginText();
        float fontSize = FONT_SIZE_FOR_TITLE_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream1.setFont(font, fontSize);
        float textSize = fontSize * font.getStringWidth(TYPE_DE_DOSSIER) / 1000;
        float offset = X_LOCATION_BEGIN_OF_FIRST_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;
        contentStream1.newLineAtOffset(offset, Y_LOCATION_OF_TITLE_OF_ALL_THREE_RECTANGULES);
        contentStream1.showText(TYPE_DE_DOSSIER);
        contentStream1.endText();
        contentStream1.close();

        PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream2.setNonStrokingColor(DARK_GRAY);
        contentStream2.beginText();
        fontSize = FONT_SIZE_FOR_CONTENT_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream2.setFont(font, fontSize);
        String applicationText = getSentenceByApplicationType(applicationType);
        textSize = fontSize * font.getStringWidth(applicationText) / 1000;
        offset = X_LOCATION_BEGIN_OF_FIRST_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;
        contentStream2.newLineAtOffset(offset, Y_LOCATION_OF_CONTENT_OF_FIRST_RECTANGULE_IN_FIRST_INDEXPAGES);
        contentStream2.showText(applicationText);
        contentStream2.endText();
        contentStream2.close();
    }

    private void addContentInSecondRectanguleInFirstTemplateOfIndexes(int indexPage, PDDocument doc, PDType0Font fontTitle, PDType0Font fontContent, String content) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(LIGHT_GRAY);
        contentStream1.beginText();
        float fontSize = FONT_SIZE_FOR_TITLE_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream1.setFont(fontTitle, fontSize);
        float textSize = fontSize * fontTitle.getStringWidth(REVENUS_MENSUELS_NETS_CUMULES) / 1000;
        float offset = (A_WIDTH_TEMPLATE - textSize) / 2;
        contentStream1.newLineAtOffset(offset, 2210);
        contentStream1.showText(REVENUS_MENSUELS_NETS_CUMULES);
        contentStream1.endText();
        contentStream1.close();

        PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream2.setNonStrokingColor(0 / 255.0F, 0 / 255.0F, 145 / 255.0F);
        contentStream2.beginText();
        fontSize = FONT_SIZE_FOR_CONTENT_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream2.setFont(fontContent, fontSize);
        textSize = fontSize * fontContent.getStringWidth(content) / 1000;
        offset = (A_WIDTH_TEMPLATE - textSize) / 2;
        contentStream2.newLineAtOffset(offset, Y_LOCATION_OF_CONTENT_OF_SECOND_RECTANGULE_IN_FIRST_INDEXPAGES);
        contentStream2.showText(content);
        contentStream2.endText();
        contentStream2.close();
    }

    private void addContentInThirdRectanguleInFirstTemplateOfIndexes(int indexPage, PDDocument doc, PDType0Font fontTitle, PDType0Font fontContent, List<Tenant> tenantList) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(LIGHT_GRAY);
        contentStream1.beginText();
        float fontSize = FONT_SIZE_FOR_TITLE_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream1.setFont(fontTitle, fontSize);
        float textSize = fontSize * fontTitle.getStringWidth(LEUR_GARANT) / 1000;
        float offset = X_LOCATION_BEGIN_OF_THIRD_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;
        contentStream1.newLineAtOffset(offset, 2210);
        contentStream1.showText(LEUR_GARANT);
        contentStream1.endText();
        contentStream1.close();

        AtomicInteger countNaturalGuarantors = new AtomicInteger();
        AtomicInteger countOrganismGuarantors = new AtomicInteger();
        AtomicInteger countLegalGuarantors = new AtomicInteger();
        AtomicInteger totalIncomingOfAllGuarantorsNaturalPerson = new AtomicInteger();
        for (Tenant tenant : tenantList) {
            Optional.ofNullable(tenant.getGuarantors())
                    .orElse(new ArrayList<>())
                    .forEach(guarantor -> {
                        TypeGuarantor typeGuarantor = guarantor.getTypeGuarantor();
                        if (typeGuarantor == TypeGuarantor.NATURAL_PERSON) {
                            countNaturalGuarantors.getAndIncrement();
                            totalIncomingOfAllGuarantorsNaturalPerson.addAndGet(calculateTotalIncomingOfGuarantor(guarantor));
                        } else if (typeGuarantor == TypeGuarantor.LEGAL_PERSON) {
                            countLegalGuarantors.getAndIncrement();
                        } else {
                            countOrganismGuarantors.getAndIncrement();
                        }
                    });
        }

        PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream2.setNonStrokingColor(LIGHT_GRAY);
        contentStream2.beginText();
        fontSize = FONT_SIZE_FOR_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream2.setFont(fontTitle, fontSize);
        float leading = LEADING_FOR_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
        contentStream2.setLeading(leading);
        float tx = X_LOCATION_OF_BEGIN_OF_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
        float ty = Y_LOCATION_OF_BEGIN_OF_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;

        if (countNaturalGuarantors.get() == 0 && countOrganismGuarantors.get() == 0 && countLegalGuarantors.get() == 0) {
            //NO guarantors
            textSize = fontSize * fontContent.getStringWidth(PAS_DE_GARANTS) / 1000;
            offset = X_LOCATION_BEGIN_OF_THIRD_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;
            contentStream2.newLineAtOffset(offset, ty);
            contentStream2.showText(PAS_DE_GARANTS);
        } else {
            if (countNaturalGuarantors.get() > 0 && countOrganismGuarantors.get() > 0 && countLegalGuarantors.get() > 0) {
                fontSize = FONT_SIZE_FOR_SMALL_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
                contentStream2.setFont(fontTitle, fontSize);
                leading = LEADING_FOR_SMALL_CONTENT_OF_THIRD_RECTANGULE_IN_FIRST_INDEXPAGES;
                contentStream2.setLeading(leading);

                contentStream2.newLineAtOffset(tx, ty + 2 * leading);
                contentStream2.showText(countNaturalGuarantors.get() + (countNaturalGuarantors.get() > 1 ? GARANTS_PHYSIQUES : GARANT_PHYSIQUE) + " ( " + totalIncomingOfAllGuarantorsNaturalPerson.get() + " € )");
                contentStream2.newLine();
                contentStream2.showText(countOrganismGuarantors.get() + (countOrganismGuarantors.get() > 1 ? ORGANISMES_GARANTS : ORGANISME_GARANT));
                contentStream2.newLine();
                contentStream2.showText(countLegalGuarantors.get() + (countLegalGuarantors.get() > 1 ? GARANTS_MORAUX : GARANT_MORAL));
            } else if (countNaturalGuarantors.get() > 0 && countOrganismGuarantors.get() > 0 && countLegalGuarantors.get() == 0) {
                contentStream2.newLineAtOffset(tx, ty + leading);
                contentStream2.showText(countNaturalGuarantors.get() + (countNaturalGuarantors.get() > 1 ? GARANTS_PHYSIQUES : GARANT_PHYSIQUE) + " ( " + totalIncomingOfAllGuarantorsNaturalPerson.get() + " € )");
                contentStream2.newLine();
                contentStream2.showText(countOrganismGuarantors.get() + (countOrganismGuarantors.get() > 1 ? ORGANISMES_GARANTS : ORGANISME_GARANT));
            } else if (countNaturalGuarantors.get() > 0 && countOrganismGuarantors.get() == 0 && countLegalGuarantors.get() > 0) {
                contentStream2.newLineAtOffset(tx, ty + leading);
                contentStream2.showText(countNaturalGuarantors.get() + (countNaturalGuarantors.get() > 1 ? GARANTS_PHYSIQUES : GARANT_PHYSIQUE) + " ( " + totalIncomingOfAllGuarantorsNaturalPerson.get() + " € )");
                contentStream2.newLine();
                contentStream2.showText(countLegalGuarantors.get() + (countLegalGuarantors.get() > 1 ? GARANTS_MORAUX : GARANT_MORAL));
            } else if (countNaturalGuarantors.get() == 0 && countOrganismGuarantors.get() > 0 && countLegalGuarantors.get() > 0) {
                contentStream2.newLineAtOffset(tx, ty + leading);
                contentStream2.showText(countOrganismGuarantors.get() + (countOrganismGuarantors.get() > 1 ? ORGANISMES_GARANTS : ORGANISME_GARANT));
                contentStream2.newLine();
                contentStream2.showText(countLegalGuarantors.get() + (countLegalGuarantors.get() > 1 ? GARANTS_MORAUX : GARANT_MORAL));
            } else if (countNaturalGuarantors.get() > 0) {
                String text = countNaturalGuarantors.get() + (countNaturalGuarantors.get() > 1 ? GARANTS_PHYSIQUES : GARANT_PHYSIQUE) + " ( " + totalIncomingOfAllGuarantorsNaturalPerson.get() + " € )";
                textSize = fontSize * fontTitle.getStringWidth(text) / 1000;
                offset = X_LOCATION_BEGIN_OF_THIRD_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;

                contentStream2.newLineAtOffset(offset, ty);
                contentStream2.showText(text);
            } else if (countOrganismGuarantors.get() > 0) {
                String text = countOrganismGuarantors.get() + (countOrganismGuarantors.get() > 1 ? ORGANISMES_GARANTS : ORGANISME_GARANT);
                textSize = fontSize * fontTitle.getStringWidth(text) / 1000;
                offset = X_LOCATION_BEGIN_OF_THIRD_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;

                contentStream2.newLineAtOffset(offset, ty);
                contentStream2.showText(text);
            } else {
                String text = countLegalGuarantors.get() + (countLegalGuarantors.get() > 1 ? GARANTS_MORAUX : GARANT_MORAL);
                textSize = fontSize * fontTitle.getStringWidth(text) / 1000;
                offset = X_LOCATION_BEGIN_OF_THIRD_RECTANGULE + (WIDTH_OF_ALL_THREE_RECTANGULES - textSize) / 2;

                contentStream2.newLineAtOffset(offset, ty);
                contentStream2.showText(text);
            }
        }

        contentStream2.endText();
        contentStream2.close();
    }

    private void addFilesOfDocumentsToDossierPDF(PDFMergerUtility ut, List<Tenant> tenantList, List<Integer> indexPagesForDocuments, PDOutlineItem pdOutlineItem) {
        for (Tenant tenant1 : tenantList) {
            boolean firstDocumentTenant = true;
            //region Adding bookmark
            PDOutlineItem pdOTenant = new PDOutlineItem();
            pdOTenant.setTitle(tenant1.getFullName());
            pdOutlineItem.addLast(pdOTenant);
            //endregion

            List<Document> documentsOrderedByCategory = tenant1.getDocuments().stream()
                    .sorted(Comparator.comparing(Document::getCreationDateTime)) // Sorted firstly by creation_date_time in order the financial documents are ordered ASC
                    .sorted(Comparator.comparing(Document::getDocumentCategory))
                    .collect(Collectors.toList());
            DocumentCategory previousCategory = documentsOrderedByCategory.get(0).getDocumentCategory();
            boolean firstDocumentSubject = true;
            int count = 0;
            for (Document document : documentsOrderedByCategory) {
                if (document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)) {
                    ++count;
                }
                DocumentCategory currentCategory = document.getDocumentCategory();
                InputStream documentInputStream = downloadDocumentService.getDocumentInputStream(document);

                //region Adding bookmark
                if (firstDocumentSubject || previousCategory != currentCategory) {
                    PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
                    destination.setPageNumber(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1));
                    destination.setTop((int) B_HEIGHT_TEMPLATE);

                    PDOutlineItem pdODocument = new PDOutlineItem();
                    pdODocument.setTitle(messageSource.getMessage(currentCategory.getLabel(), null, locale));
                    pdODocument.setDestination(destination);

                    if (firstDocumentTenant) {
                        pdOTenant.setDestination(destination);
                        firstDocumentTenant = false;
                    }
                    pdOTenant.addLast(pdODocument);
                }
                //endregion

                //We get here the second sentence located in the header of attachment pages for the current tenant
                String sentence = getSentenceForTenantFromDocumentCategory(tenant1, currentCategory, count);
                addDocument(ut, documentInputStream, indexPagesForDocuments, firstDocumentSubject || previousCategory != currentCategory, tenantList, sentence);
                firstDocumentSubject = false;
                previousCategory = currentCategory;
            }

            List<Guarantor> guarantorsOrderedByType = tenant1.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor)).collect(Collectors.toList());
            int counterOfGuarantor = guarantorsOrderedByType.size() > 1 ? 1 : 0;
            for (Guarantor guarantor1 : guarantorsOrderedByType) {
                boolean firstDocument = true;
                //region Adding bookmark
                PDOutlineItem pdOGuarantor = new PDOutlineItem();
                if (guarantor1.getTypeGuarantor().equals(TypeGuarantor.NATURAL_PERSON)) {
                    pdOGuarantor.setTitle(guarantor1.getFirstName() + ' ' + guarantor1.getLastName());
                } else if (guarantor1.getTypeGuarantor().equals(TypeGuarantor.LEGAL_PERSON)) {
                    pdOGuarantor.setTitle(TITLE_FOR_GUARANTOR_LEGAL_PERSON);
                } else {
                    pdOGuarantor.setTitle(TITLE_FOR_GUARANTOR_ORGANISM);
                }

                pdOTenant.addLast(pdOGuarantor);
                //endregion

                documentsOrderedByCategory = guarantor1.getDocuments().stream()
                        .sorted(Comparator.comparing(Document::getCreationDateTime)) // Sorted firstly by creation_date_time in order the financial documents are ordered ASC
                        .sorted(Comparator.comparing(Document::getDocumentCategory))
                        .collect(Collectors.toList());
                previousCategory = documentsOrderedByCategory.get(0).getDocumentCategory();
                firstDocumentSubject = true;
                int counter = 0;
                for (Document document : documentsOrderedByCategory) {
                    if (document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)) {
                        ++counter;
                    }
                    DocumentCategory currentCategory = document.getDocumentCategory();
                    InputStream documentInputStream = downloadDocumentService.getDocumentInputStream(document);

                    //region Adding bookmark
                    if (firstDocumentSubject || previousCategory != currentCategory) {
                        PDPageFitWidthDestination destination = new PDPageFitWidthDestination();
                        destination.setPageNumber(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1));
                        destination.setTop((int) B_HEIGHT_TEMPLATE);

                        PDOutlineItem pdODocument = new PDOutlineItem();
                        if (guarantor1.getTypeGuarantor() == TypeGuarantor.ORGANISM && currentCategory == DocumentCategory.IDENTIFICATION) {
                            pdODocument.setTitle(messageSource.getMessage("tenant.profile.link6.v2", null, locale));
                        } else if (guarantor1.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON) {
                            if (currentCategory == DocumentCategory.IDENTIFICATION) {
                                pdODocument.setTitle(messageSource.getMessage("tenant.profile.link7.v2", null, locale));
                            } else { //DocumentCategory.IDENTIFICATION_LEGAL_PERSON
                                pdODocument.setTitle(messageSource.getMessage("tenant.profile.link8.v2", null, locale));
                            }
                        } else {
                            pdODocument.setTitle(messageSource.getMessage(currentCategory.getLabel(), null, locale));
                        }
                        pdODocument.setDestination(destination);

                        if (firstDocument) {
                            pdOGuarantor.setDestination(destination);
                            firstDocument = false;
                        }

                        pdOGuarantor.addLast(pdODocument);
                    }
                    //endregion

                    //We get here the second sentence located in the header of attachment pages for the current guarantor
                    String sentence = getSentenceForGuarantorFromDocumentCategory(counterOfGuarantor, guarantor1.getTypeGuarantor(), currentCategory, tenant1.getFirstName(), counter);
                    addDocument(ut, documentInputStream, indexPagesForDocuments, firstDocumentSubject || previousCategory != currentCategory, tenantList, sentence);
                    firstDocumentSubject = false;
                    previousCategory = currentCategory;

                }
                if (guarantorsOrderedByType.size() > 1) {
                    counterOfGuarantor++;
                }
            }
        }
    }

    private float addIndexesOfDocumentsOfTenantInCurrentPage(Tenant tenant, int indexPage, List<Integer> indexPagesForDocuments, AtomicInteger iteratorInIndexPagesForDocuments, PDDocument doc, PDType0Font fontForTitleAndSalary, PDType0Font fontIndexLines, float marginX, float yLocationFirstContentStream, float yLocationSecondContentStream, float yLocationTenantEmailContentStream, float yLocationThirdContentStream, float xLocationOfEndOfRectangule) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(GREEN);
        contentStream1.beginText();
        float fontSize = FONT_SIZE_FOR_TITLE_OF_GROUP_OF_INDEXES;
        contentStream1.setFont(fontForTitleAndSalary, fontSize);
        float textSize = fontSize * fontForTitleAndSalary.getStringWidth(LES_PIECES_JUSTIFICATIVES) / 1000;
        float offset = marginX + (WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES - textSize) / 2;
        contentStream1.newLineAtOffset(offset, yLocationFirstContentStream);
        contentStream1.showText(LES_PIECES_JUSTIFICATIVES);
        contentStream1.endText();
        contentStream1.close();

        PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream2.setNonStrokingColor(GRAY);
        contentStream2.beginText();
        contentStream2.setFont(fontForTitleAndSalary, fontSize);
        String tenantFullName = tenant.getFullName();
        textSize = fontSize * fontForTitleAndSalary.getStringWidth(tenantFullName) / 1000;
        offset = marginX + (WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES - textSize) / 2;
        contentStream2.newLineAtOffset(offset, yLocationSecondContentStream);
        contentStream2.showText(tenantFullName);
        contentStream2.endText();
        contentStream2.close();

        PDPageContentStream contentStreamEmailTenant = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStreamEmailTenant.setNonStrokingColor(GRAY);
        contentStreamEmailTenant.beginText();
        fontSize = FONT_SIZE_FOR_CONTENT_OF_GROUP_OF_INDEXES;
        contentStreamEmailTenant.setFont(fontIndexLines, fontSize);
        String tenantEmail = tenant.getEmail();
        textSize = fontSize * fontIndexLines.getStringWidth(tenantEmail) / 1000;
        offset = marginX + (WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES - textSize) / 2;
        contentStreamEmailTenant.newLineAtOffset(offset, yLocationTenantEmailContentStream);
        contentStreamEmailTenant.showText(tenantEmail);
        contentStreamEmailTenant.endText();
        contentStreamEmailTenant.close();

        PDPageContentStream contentStream3 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream3.setNonStrokingColor(LIGHT_GRAY);
        contentStream3.beginText();
        contentStream3.setFont(fontIndexLines, fontSize);
        contentStream3.newLineAtOffset(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES, yLocationThirdContentStream);
        float leading = LEADING_FOR_CONTENT_OF_GROUP_OF_INDEXES;
        contentStream3.setLeading(leading);

        List<PDAnnotation> annotationList = doc.getPage(indexPage).getAnnotations();
        float yLocationDocFinancialIndex = -1;
        float yLocationDocTaxIndex = -1;
        float lastYLocation = yLocationThirdContentStream;
        //We obtain here the list with the categories, NOT REPEATED (distinctByKey), of documents that the tenant has. Ordered ascending by the ID of DocumentCategory.
        List<DocumentCategory> listOfDocumentCategoryContainedForTenant = tenant.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).filter(distinctByKey(Document::getDocumentCategory)).map(Document::getDocumentCategory).collect(Collectors.toList());
        for (DocumentCategory documentCategory : listOfDocumentCategoryContainedForTenant) {
            int numberOfFirstPageForCurrentTypeOfDocument = indexPagesForDocuments.get(iteratorInIndexPagesForDocuments.getAndIncrement() - 1) + 1;
            String indexText = "p." + numberOfFirstPageForCurrentTypeOfDocument + " - " + messageSource.getMessage(documentCategory.getLabel(), null, locale);
            contentStream3.showText(indexText);
            contentStream3.newLine();

            //region Adding link to page
            PDPage pageToNavigate = doc.getPage(numberOfFirstPageForCurrentTypeOfDocument - 1);

            PDPageFitWidthDestination destinationPage = new PDPageFitWidthDestination();
            destinationPage.setPage(pageToNavigate);
            destinationPage.setTop((int) pageToNavigate.getMediaBox().getHeight());

            PDActionGoTo action = new PDActionGoTo();
            action.setDestination(destinationPage);

            PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
            borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
            borderULine.setWidth(0);

            PDRectangle position = new PDRectangle();
            position.setLowerLeftX(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES);
            position.setLowerLeftY(lastYLocation);
            float indexTextWidth = fontSize * fontIndexLines.getStringWidth(indexText) / 1000;
            position.setUpperRightX(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES + indexTextWidth);
            position.setUpperRightY(lastYLocation + fontSize);

            PDAnnotationLink pageLink = new PDAnnotationLink();
            pageLink.setBorderStyle(borderULine);
            pageLink.setRectangle(position);
            pageLink.setAction(action);

            annotationList.add(pageLink);
            //endregion

            if (documentCategory == DocumentCategory.FINANCIAL) {
                yLocationDocFinancialIndex = lastYLocation;
            }
            if (documentCategory == DocumentCategory.TAX && tenant.getAllowCheckTax()) {
                yLocationDocTaxIndex = lastYLocation;
            }

            lastYLocation -= leading;
        }
        contentStream3.endText();
        contentStream3.close();

        for (PDAnnotation annotation : annotationList) {
            annotation.constructAppearances(doc);
        }

        if (yLocationDocFinancialIndex != -1) {
            PDPageContentStream cSSalaryTenant = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            cSSalaryTenant.setNonStrokingColor(43 / 255.0F, 139 / 255.0F, 247 / 255.0F);
            cSSalaryTenant.beginText();
            fontSize = FONT_SIZE_FOR_INCOMING_NEXT_TO_INDEX_OF_FINANCIAL_DOCUMENTS;
            cSSalaryTenant.setFont(fontForTitleAndSalary, fontSize);
            String text = tenant.getTotalSalary() + " € net";
            textSize = fontSize * fontForTitleAndSalary.getStringWidth(text) / 1000;
            offset = xLocationOfEndOfRectangule - textSize - RIGHT_MARGIN_FOR_TEXT_INCOMING;
            cSSalaryTenant.newLineAtOffset(offset, yLocationDocFinancialIndex);
            cSSalaryTenant.showText(text);
            cSSalaryTenant.endText();
            cSSalaryTenant.close();
        }

        if (yLocationDocTaxIndex != -1) {
            fontSize = FONT_SIZE_SMALL;
            String text = "Revenu fiscal certifié";
            textSize = fontSize * fontForTitleAndSalary.getStringWidth(text) / 1000;

            PDPageContentStream greenBackground = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            greenBackground.setNonStrokingColor(LIGHT_GREEN);
            float x = xLocationOfEndOfRectangule - textSize - RIGHT_MARGIN_FOR_TEXT_INCOMING - 20 - DGFIP_ICON_WIDTH;
            float y = yLocationDocTaxIndex + 7;
            float height = 45;
            float r = height / 2;
            float width = textSize + RIGHT_MARGIN_FOR_TEXT_INCOMING - 35;

            final float k = 0.552284749831f;
            greenBackground.moveTo(x - r, y);
            greenBackground.curveTo(x - r, y + k * r, x - k * r, y + r, x, y + r);
            greenBackground.curveTo(x + width / 3, y + r, x + width * 2 / 3, y + r, x + width, y + r);
            greenBackground.curveTo(x + width + k * r, y + r, x + width + r, y + k * r, x + width + r, y);
            greenBackground.curveTo(x + width + r, y - k * r, x + width + k * r, y - r, x + width, y - r);
            greenBackground.curveTo(x + width * 2 / 3, y - r, x + width / 3, y - r, x, y - r);
            greenBackground.curveTo(x - k * r, y - r, x - r, y - k * r, x-r, y);
            greenBackground.fill();

            greenBackground.fill();
            greenBackground.close();


            PDPageContentStream cSSalaryTenant = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            cSSalaryTenant.setNonStrokingColor(DARK_GREEN);
            cSSalaryTenant.beginText();
            cSSalaryTenant.setFont(fontIndexLines, fontSize);
            offset = xLocationOfEndOfRectangule - textSize - RIGHT_MARGIN_FOR_TEXT_INCOMING - DGFIP_ICON_WIDTH;
            cSSalaryTenant.newLineAtOffset(offset, yLocationDocTaxIndex);
            cSSalaryTenant.showText(text);
            cSSalaryTenant.endText();
            cSSalaryTenant.close();

            BufferedImage bimVerified = ImageIO.read(verifiedIcon.getInputStream());
            PDImageXObject imgVerified = LosslessFactory.createFromImage(doc, bimVerified);
            PDPageContentStream contentStream = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            offset = xLocationOfEndOfRectangule - textSize - RIGHT_MARGIN_FOR_TEXT_INCOMING - 28 - DGFIP_ICON_WIDTH;
            contentStream.drawImage(imgVerified, offset, yLocationDocTaxIndex - 4);
            contentStream.close();

            BufferedImage bim = ImageIO.read(dgfipIcon.getInputStream());
            PDImageXObject dgfipImg = LosslessFactory.createFromImage(doc, bim);
            PDPageContentStream dgfipContentStream = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            offset = xLocationOfEndOfRectangule - RIGHT_MARGIN_FOR_TEXT_INCOMING - 20;
            dgfipContentStream.drawImage(dgfipImg, offset, yLocationDocTaxIndex - 14);
            dgfipContentStream.close();

        }

        return lastYLocation;
    }

    private float addIndexesOfDocumentsOfGuarantorInCurrentPage(Guarantor guarantor, int indexPage, List<Integer> indexPagesForDocuments, AtomicInteger iteratorInIndexPagesForDocuments, PDDocument doc, PDType0Font fontLinesOfTitleAndTextOfIncomingGuarantor, PDType0Font fontLinesOfDocumentIndexes, float marginX, float lastYLocation, float xLocationOfEndOfRectangule) throws IOException {
        PDPageContentStream contentStream1 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream1.setNonStrokingColor(GREEN);
        contentStream1.beginText();
        float fontSize = FONT_SIZE_FOR_TITLE_OF_GROUP_OF_INDEXES;
        contentStream1.setFont(fontLinesOfTitleAndTextOfIncomingGuarantor, fontSize);
        float textSize = fontSize * fontLinesOfTitleAndTextOfIncomingGuarantor.getStringWidth(LES_PIECES_JUSTIFICATIVES_DE_SON_GARANT) / 1000;
        float offset = marginX + (WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES - textSize) / 2;
        float yLocation = lastYLocation - SEPARATION_BETWEEN_GROUPS_OF_DOCUMENT_INDEXES;
        contentStream1.newLineAtOffset(offset, yLocation);

        contentStream1.showText(LES_PIECES_JUSTIFICATIVES_DE_SON_GARANT);

        contentStream1.endText();
        contentStream1.close();

        TypeGuarantor typeGuarantor = guarantor.getTypeGuarantor();

        //region Name of guarantor if (NATURAL_PERSON or LEGAL_PERSON)
        if (typeGuarantor == TypeGuarantor.NATURAL_PERSON || typeGuarantor == TypeGuarantor.LEGAL_PERSON) {
            PDPageContentStream contentStream2 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            contentStream2.setNonStrokingColor(GRAY);
            contentStream2.beginText();
            contentStream2.setFont(fontLinesOfTitleAndTextOfIncomingGuarantor, fontSize);
            String guarantorFullName = guarantor.getCompleteName();
            textSize = fontSize * fontLinesOfTitleAndTextOfIncomingGuarantor.getStringWidth(guarantorFullName) / 1000;
            offset = marginX + (WIDTH_OF_THE_TWO_COLUMNS_FOR_INDEXES - textSize) / 2;
            yLocation = yLocation - (SEPARATION_BETWEEN_TITLE_AND_NAME_OF_SUBJECT_IN_GROUP_OF_DOCUMENT_INDEXES);
            contentStream2.newLineAtOffset(offset, yLocation);

            contentStream2.showText(guarantorFullName);

            contentStream2.endText();
            contentStream2.close();
        }
        //endregion

        PDPageContentStream contentStream3 = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
        contentStream3.setNonStrokingColor(LIGHT_GRAY);
        contentStream3.beginText();
        fontSize = FONT_SIZE_FOR_CONTENT_OF_GROUP_OF_INDEXES;
        contentStream3.setFont(fontLinesOfDocumentIndexes, fontSize);
        yLocation = yLocation - (SEPARATION_BETWEEN_NAME_OF_SUBJECT_AND_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES);
        contentStream3.newLineAtOffset(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES, yLocation);
        float leading = LEADING_FOR_CONTENT_OF_GROUP_OF_INDEXES;
        contentStream3.setLeading(leading);

        List<PDAnnotation> annotationList = doc.getPage(indexPage).getAnnotations();
        float yLocationDocFinancialIndex = -1;
        //We obtain here the list with the categories, NOT REPEATED (distinctByKey), of documents that the guarantor has. Ordered ascending by the ID of DocumentCategory.
        List<DocumentCategory> listOfDocumentCategoryContainedForGuarantor = guarantor.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).filter(distinctByKey(Document::getDocumentCategory)).map(Document::getDocumentCategory).collect(Collectors.toList());
        for (DocumentCategory documentCategory : listOfDocumentCategoryContainedForGuarantor) {
            int numberOfFirstPageForCurrentTypeOfDocument = indexPagesForDocuments.get(iteratorInIndexPagesForDocuments.getAndIncrement() - 1) + 1;
            String indexText = "p." + numberOfFirstPageForCurrentTypeOfDocument + " - ";

            if (guarantor.getTypeGuarantor() == TypeGuarantor.ORGANISM && documentCategory == DocumentCategory.IDENTIFICATION) {
                indexText += messageSource.getMessage("tenant.profile.link6.v2", null, locale);
            } else if (guarantor.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON) {
                if (documentCategory == DocumentCategory.IDENTIFICATION) {
                    indexText += messageSource.getMessage("tenant.profile.link7.v2", null, locale);
                } else { //DocumentCategory.IDENTIFICATION_LEGAL_PERSON
                    indexText += messageSource.getMessage("tenant.profile.link8.v2", null, locale);
                }
            } else {
                indexText += messageSource.getMessage(documentCategory.getLabel(), null, locale);
            }
            contentStream3.showText(indexText);
            contentStream3.newLine();

            //region Adding link to page
            PDPage pageToNavigate = doc.getPage(numberOfFirstPageForCurrentTypeOfDocument - 1);

            PDPageFitWidthDestination destinationPage = new PDPageFitWidthDestination();
            destinationPage.setPage(pageToNavigate);
            destinationPage.setTop((int) pageToNavigate.getMediaBox().getHeight());

            PDActionGoTo action = new PDActionGoTo();
            action.setDestination(destinationPage);

            PDBorderStyleDictionary borderULine = new PDBorderStyleDictionary();
            borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE);
            borderULine.setWidth(0);

            PDRectangle position = new PDRectangle();
            position.setLowerLeftX(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES);
            position.setLowerLeftY(yLocation);
            float indexTextWidth = fontSize * fontLinesOfDocumentIndexes.getStringWidth(indexText) / 1000;
            position.setUpperRightX(marginX + ADDITIONAL_LEFT_MARGIN_FOR_CONTENT_OF_INDEXES + indexTextWidth);
            position.setUpperRightY(yLocation + fontSize);

            PDAnnotationLink pageLink = new PDAnnotationLink();
            pageLink.setBorderStyle(borderULine);
            pageLink.setRectangle(position);
            pageLink.setAction(action);

            annotationList.add(pageLink);
            //endregion

            if (documentCategory == DocumentCategory.FINANCIAL) {
                yLocationDocFinancialIndex = yLocation;
            }

            yLocation -= leading;
        }

        contentStream3.endText();
        contentStream3.close();

        for (PDAnnotation annotation : annotationList) {
            annotation.constructAppearances(doc);
        }

        if (yLocationDocFinancialIndex != -1) {
            PDPageContentStream cSIncomingGuarantor = new PDPageContentStream(doc, doc.getPage(indexPage), PDPageContentStream.AppendMode.APPEND, true);
            cSIncomingGuarantor.setNonStrokingColor(43 / 255.0F, 139 / 255.0F, 247 / 255.0F);
            cSIncomingGuarantor.beginText();
            fontSize = FONT_SIZE_FOR_INCOMING_NEXT_TO_INDEX_OF_FINANCIAL_DOCUMENTS;
            cSIncomingGuarantor.setFont(fontLinesOfTitleAndTextOfIncomingGuarantor, fontSize);
            String text = calculateTotalIncomingOfGuarantor(guarantor) + " € net";
            textSize = fontSize * fontLinesOfTitleAndTextOfIncomingGuarantor.getStringWidth(text) / 1000;
            offset = xLocationOfEndOfRectangule - textSize - RIGHT_MARGIN_FOR_TEXT_INCOMING;
            cSIncomingGuarantor.newLineAtOffset(offset, yLocationDocFinancialIndex);
            cSIncomingGuarantor.showText(text);
            cSIncomingGuarantor.endText();
            cSIncomingGuarantor.close();
        }

        return yLocation;
    }

    private void checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(long apartmentSharingId, String token) {
        int numberOfTenants = tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(apartmentSharingId);
        if (numberOfTenants > 0) {
            throw new ApartmentSharingUnexpectedException(token);
        }
    }

    @Override
    public InputStream render(ApartmentSharing apartmentSharing) throws IOException {

        checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(apartmentSharing.getId(), apartmentSharing.getToken());

        PDFMergerUtility ut = new PDFMergerUtility();
        List<Tenant> tenantList = apartmentSharing.getTenants().stream().sorted(Comparator.comparing(Tenant::getTenantType)).collect(Collectors.toList());
        int numberOfTenants = tenantList.size();

        List<Integer> indexPagesForDocuments = new ArrayList<>();

        //region Adding bookmarks
        PDDocumentOutline pdDocumentOutline = new PDDocumentOutline();

        PDOutlineItem pdOutlineItem = new PDOutlineItem();
        pdOutlineItem.setTitle(TITLE_0_DOSSIER_PDF);

        PDPageFitWidthDestination destination_title = new PDPageFitWidthDestination();
        destination_title.setPageNumber(0);
        destination_title.setTop((int) B_HEIGHT_TEMPLATE);
        pdOutlineItem.setDestination(destination_title);
        pdDocumentOutline.addLast(pdOutlineItem);
        //endregion

        createFirstsPages(ut, numberOfTenants, indexPagesForDocuments, pdOutlineItem);

        Tenant mainTenant = tenantList.stream()
                .filter(t -> t.getTenantType() == TenantType.CREATE)
                .findFirst()
                .orElseThrow(() -> new TenantNotFoundException(TenantType.CREATE));
        addDocumentOfClarification(ut, tenantList, mainTenant, indexPagesForDocuments, pdOutlineItem);

        //region Add files of documents to Dossier PDF
        addFilesOfDocumentsToDossierPDF(ut, tenantList, indexPagesForDocuments, pdOutlineItem);
        //endregion

        ByteArrayOutputStream merge = new ByteArrayOutputStream();
        ut.setDestinationStream(merge);
        try {
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error("Problem merge document for pdf full");
            log.error(e.getMessage(), e.getCause());
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(merge.toByteArray()))) {

            doc.getDocumentCatalog().setDocumentOutline(pdDocumentOutline);
            pdOutlineItem.openNode();
            pdDocumentOutline.openNode();

            addPaginate(doc);

            //region Adding content to First pages
            PDType0Font fontSpectralExtraBold = PDType0Font.load(doc, FONT_SPECTRAL_EXTRA_BOLD.getInputStream());
            PDType0Font fontMarianneRegular = PDType0Font.load(doc, FONT_MARIANNE_REGULAR.getInputStream());
            PDType0Font fontMarianneBold = PDType0Font.load(doc, FONT_MARIANNE_BOLD.getInputStream());

            /* Initialized in 2, because this is the position in the array (indexPagesForDocuments)
            where it is located the number of the page located before the beginning of attachment pages.
            - (position 1) ------> indexPagesForDocuments[0] = Number of index pages. For 1 or 2 tenants then 1 page, for 3 or 4 tenants then 2 pages, ...
            - (position 2) ------> indexPagesForDocuments[1] = Page number where the clarification page is.
            -              ------> indexPagesForDocuments[1] + 1 = page number of the first attachment page.
            - (last position) ---> indexPagesForDocuments[indexPagesForDocuments.size() - 1] = doc.getNumberOfPages() */
            AtomicInteger iteratorInIndexPagesForDocuments = new AtomicInteger(2);

            int indexTenant = 0;
            int totalPages = doc.getNumberOfPages();
            for (int indexPage = 0; indexPage < totalPages && indexTenant < numberOfTenants; indexPage++) {

                addFirstNamesOfTenantsInTheHeaderOfCurrentIndexPage(indexPage, doc, tenantList, fontSpectralExtraBold);

                Tenant leftTenantInPage = tenantList.get(indexTenant);
                Tenant rightTenantInPage = (indexTenant + 1) < numberOfTenants ? tenantList.get(indexTenant + 1) : null;

                float yLocationFirstContentStream, yLocationSecondContentStream, yLocationTenantEmailContentStream, yLocationThirdContentStream;

                //The content area in the first page (indexPage == 0)
                // is different in comparision with the one available in the other pages of indexes
                if (indexPage == 0) {
                    //region Static Text (Le dossier en un clin d’oeil)
                    addStaticTextInFirstTemplateOfIndexes(indexPage, doc, fontSpectralExtraBold);
                    //endregion
                    //region Text Box (Type de dossier)
                    addContentInFirstRectanguleInFirstTemplateOfIndexes(indexPage, doc, fontMarianneRegular, apartmentSharing.getApplicationType());
                    //endregion
                    //region Text Box (Revenus mensuels nets cumulés)
                    addContentInSecondRectanguleInFirstTemplateOfIndexes(indexPage, doc, fontMarianneRegular, fontMarianneBold, apartmentSharing.totalSalary() + " €");
                    //endregion
                    //region Text Box (Leur garant)
                    addContentInThirdRectanguleInFirstTemplateOfIndexes(indexPage, doc, fontMarianneRegular, fontMarianneBold, tenantList);
                    //endregion

                    //region ContentStreams locations on the y-axis in first page of indexes
                    yLocationFirstContentStream = Y_LOCATION_OF_TITLE_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
                    yLocationSecondContentStream = Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
                    yLocationTenantEmailContentStream = Y_LOCATION_OF_EMAIL_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
                    yLocationThirdContentStream = Y_LOCATION_OF_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_FIRST_INDEXPAGE;
                    //endregion
                } else {
                    //region ContentStreams locations on the y-axis in the other pages of indexes
                    yLocationFirstContentStream = Y_LOCATION_OF_TITLE_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE;
                    yLocationSecondContentStream = Y_LOCATION_OF_NAME_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE;
                    yLocationTenantEmailContentStream = Y_LOCATION_OF_EMAIL_OF_TENANT_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE;
                    yLocationThirdContentStream = Y_LOCATION_OF_INDEX_PAGES_IN_GROUP_OF_DOCUMENT_INDEXES_FOR_TENANTS_IN_SECOND_INDEXPAGE;
                    //endregion
                }

                //region Details of left Tenant in current page
                float lastYLocationLeftSide = addIndexesOfDocumentsOfTenantInCurrentPage(leftTenantInPage, indexPage, indexPagesForDocuments, iteratorInIndexPagesForDocuments, doc, fontSpectralExtraBold, fontMarianneRegular, LEFT_MARGIN_FOR_LEFT_TENANT, yLocationFirstContentStream, yLocationSecondContentStream, yLocationTenantEmailContentStream, yLocationThirdContentStream, X_LOCATION_OF_END_OF_LEFT_RECTANGULE_IN_INDEXPAGES);
                List<Guarantor> guarantorsLeftTenant = leftTenantInPage.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor)).collect(Collectors.toList());
                for (Guarantor guarantor : guarantorsLeftTenant) {
                    lastYLocationLeftSide = addIndexesOfDocumentsOfGuarantorInCurrentPage(guarantor, indexPage, indexPagesForDocuments, iteratorInIndexPagesForDocuments, doc, fontSpectralExtraBold, fontMarianneRegular, LEFT_MARGIN_FOR_LEFT_TENANT, lastYLocationLeftSide, X_LOCATION_OF_END_OF_LEFT_RECTANGULE_IN_INDEXPAGES);
                }
                //endregion
                //region Details of right Tenant in current page
                if (rightTenantInPage != null) {
                    float lastYLocationRightSide = addIndexesOfDocumentsOfTenantInCurrentPage(rightTenantInPage, indexPage, indexPagesForDocuments, iteratorInIndexPagesForDocuments, doc, fontSpectralExtraBold, fontMarianneRegular, LEFT_MARGIN_FOR_RIGHT_TENANT, yLocationFirstContentStream, yLocationSecondContentStream, yLocationTenantEmailContentStream, yLocationThirdContentStream, X_LOCATION_OF_END_OF_RIGHT_RECTANGULE_IN_INDEXPAGES);
                    List<Guarantor> guarantorsRightTenant = rightTenantInPage.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor)).collect(Collectors.toList());
                    for (Guarantor guarantor : guarantorsRightTenant) {
                        lastYLocationRightSide = addIndexesOfDocumentsOfGuarantorInCurrentPage(guarantor, indexPage, indexPagesForDocuments, iteratorInIndexPagesForDocuments, doc, fontSpectralExtraBold, fontMarianneRegular, LEFT_MARGIN_FOR_RIGHT_TENANT, lastYLocationRightSide, X_LOCATION_OF_END_OF_RIGHT_RECTANGULE_IN_INDEXPAGES);
                    }
                }
                //endregion

                indexTenant += 2;
            }
            //endregion
            doc.save(result);
            log.info("Generation completed");
        } catch (IOException e) {
            log.error("Problem creating full pdf", e);
            throw e;
        }
        // optimisation
        try (ByteArrayOutputStream finalResult = new ByteArrayOutputStream();
             PDDocument originDocument = PDDocument.load(result.toByteArray())) {

            new PdfOptimizer().optimize(originDocument);
            originDocument.save(finalResult);

            return new ByteArrayInputStream(finalResult.toByteArray());
        } catch (Exception e) {
            log.warn("Optimisation FAILED !", e);
        }
        return new ByteArrayInputStream(result.toByteArray());

    }
}
