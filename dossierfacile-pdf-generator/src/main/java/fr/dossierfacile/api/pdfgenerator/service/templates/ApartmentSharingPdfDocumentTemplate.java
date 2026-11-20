package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.pdfgenerator.exception.TenantNotFoundException;
import fr.dossierfacile.api.pdfgenerator.model.TargetImageData;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DownloadService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfSignatureService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.ValidatedDossierPdfRenderer;
import fr.dossierfacile.api.pdfgenerator.util.Fonts;
import fr.dossierfacile.api.pdfgenerator.util.PdfOptimizer;
import fr.dossierfacile.api.pdfgenerator.util.Utility;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
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
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.Matrix;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static fr.dossierfacile.api.pdfgenerator.service.templates.PdfFileTemplate.*;
import static org.apache.pdfbox.multipdf.PDFMergerUtility.DocumentMergeMode;
import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingPdfDocumentTemplate implements PdfTemplate<ApartmentSharing> {

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
    private static final String TENANT_RESIDENCY_TEXT_HEADER = "Le justificatif de situation d'hébergement de ";
    private static final String TENANT_PROFESSIONAL_TEXT_HEADER = "Le justificatif de situation professionnelle de ";
    private static final String TENANT_FINANCIAL_TEXT_HEADER = "Le justificatif de ressources n° ";
    private static final String TENANT_TAX_TEXT_HEADER = "L'avis d'imposition de ";
    private static final String NATURAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "La pièce d'identité du garant ";
    private static final String NATURAL_GUARANTOR_RESIDENCY_TEXT_HEADER = "Le justificatif de situation d'hébergement du garant ";
    private static final String NATURAL_GUARANTOR_PROFESSIONAL_TEXT_HEADER = "Le justificatif de situation professionnelle du garant ";
    private static final String NATURAL_GUARANTOR_FINANCIAL_TEXT_HEADER = "Le justificatif de ressources n° ";
    private static final String NATURAL_GUARANTOR_TAX_TEXT_HEADER = "L'avis d'imposition du garant ";
    private static final String LEGAL_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "Identité de la personne morale qui se porte garant pour ";
    private static final String LEGAL_GUARANTOR_LEGAL_IDENTIFICATION_TEXT_HEADER = "Justificatif d'identité du représentant de la personne morale qui se porte garant pour ";
    private static final String ORGANISM_GUARANTOR_IDENTIFICATION_TEXT_HEADER = "L'attestation de l'organisme qui se porte garant pour ";
    //endregion

    //region Static sentences
    private static final String LE_MOT_DU_LOCATAIRE = "Le mot du locataire";
    private static final String TITLE_FOR_GUARANTOR_LEGAL_PERSON = "Garant personne morale";
    private static final String TITLE_FOR_GUARANTOR_ORGANISM = "Organisme garant";
    //endregion

    //region Other dimensions
    //--------------------------------------------------------------------------------------------------
    //First page of indexes
    //--------------------------------------------------------------------------------------------------
    //Second page of indexes
    //--------------------------------------------------------------------------------------------------
    //Common dimensions

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

    // Bottom of the band holding the RF and DossierFacile logos at the top of the
    // attachments template (just above the text headers drawn by code)
    private static final float Y_BOTTOM_OF_HEADER_LOGOS_AREA = B_HEIGHT_TEMPLATE / 421 * 365f;

    //endregion

    //region Bookmark's titles
    private static final String TITLE_0_DOSSIER_PDF = "Dossier PDF";
    private static final String TITLE_2_CLARIFICATION = "Clarification";
    //endregion

    private static final Color GREEN = new Color(0, 172, 140);

    private static final Color DARK_GRAY = new Color(30, 30, 30);
    private static final Color GRAY = new Color(56, 56, 56);

    private final Locale locale = LocaleContextHolder.getLocale();
    private final TenantCommonRepository tenantRepository;
    private final DownloadService downloadDocumentService;
    private final MessageSource messageSource;
    private final PdfSignatureService pdfSignatureService;
    private final ValidatedDossierPdfRenderer validatedDossierPdfRenderer;

    private void addPaginate(PDDocument doc) throws IOException {

        PDFont font = new PDType1Font(FontName.HELVETICA_BOLD);
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

    private ByteArrayOutputStream addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(List<Tenant> tenantList, String headerSentence,
            String bodyText) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PDDocument doc = ATTACHMENTS_AND_CLARIFICATIONS.load()) {

            //region Reading fonts
            PDType0Font font1 = Fonts.MARIANNE_LIGHT.load(doc);
            PDType0Font font2 = Fonts.SPECTRAL_BOLD.load(doc);
            PDType0Font font3 = Fonts.SPECTRAL_EXTRA_BOLD.load(doc);
            PDType0Font font4 = Fonts.SPECTRAL_ITALIC.load(doc);
            PDType0Font font5 = Fonts.NOTO_EMOJI_MEDIUM.load(doc);
            //endregion

            PDPage pageTemplate = doc.getPage(0);

            // A non verified dossier (TO_PROCESS or COMPLETED) must not carry the République
            // Française and DossierFacile logos: cover the top band of the template
            // before drawing the text headers
            try (PDPageContentStream cover = new PDPageContentStream(doc, pageTemplate, PDPageContentStream.AppendMode.APPEND, true)) {
                cover.setNonStrokingColor(Color.WHITE);
                cover.addRect(0, Y_BOTTOM_OF_HEADER_LOGOS_AREA, A_WIDTH_TEMPLATE, B_HEIGHT_TEMPLATE - Y_BOTTOM_OF_HEADER_LOGOS_AREA);
                cover.fill();
            }

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
                contentStream4.endText();

                float marginX = LEFT_MARGIN_FOR_CLARIFICATION_TEXT;
                Utility.addText(
                        contentStream4,
                        C_WIDTH_AVAILABLE_AREA_TEMPLATE - 2 * marginX,
                        X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE + marginX,
                        B_HEIGHT_TEMPLATE - Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE,
                        bodyText,
                        font4,
                        FONT_SIZE_FOR_CLARIFICATION_TEXT,
                        font5
                );
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

    private String getSentenceForGuarantorFromDocumentCategory(int numberOfGuarantor, TypeGuarantor typeGuarantor, DocumentCategory documentCategory,
            String tenantFirstName, int count) {
        switch (typeGuarantor) {
            case NATURAL_PERSON -> {
                String followingPartOfSentence = (numberOfGuarantor > 0 ? "n° " + numberOfGuarantor + " " : "") + "de " + tenantFirstName;

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

    private TargetImageData adjustSourceImageToAvailableAreaInTemplate(float sourceWidth, float sourceHeight) {
        float ratioSource = sourceHeight / sourceWidth;
        float ratioAvailableAreaInTemplate = D_HEIGHT_AVAILABLE_AREA_TEMPLATE / C_WIDTH_AVAILABLE_AREA_TEMPLATE;

        if (ratioSource <= ratioAvailableAreaInTemplate) {
            float targetWidth = C_WIDTH_AVAILABLE_AREA_TEMPLATE;
            float targetHeight = C_WIDTH_AVAILABLE_AREA_TEMPLATE * sourceHeight / sourceWidth;
            float horizontalDisplacement = X_MARGIN_RIGHT_AVAILABLE_AREA_TEMPLATE;
            float verticalDisplacement = B_HEIGHT_TEMPLATE - (Y_MARGIN_TOP_AVAILABLE_AREA_TEMPLATE + D_HEIGHT_AVAILABLE_AREA_TEMPLATE)
                    + (D_HEIGHT_AVAILABLE_AREA_TEMPLATE - targetHeight) / 2;
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
            TargetImageData targetImageData = adjustSourceImageToAvailableAreaInTemplate(innerPage.getMediaBox().getWidth(),
                    innerPage.getMediaBox().getHeight());
            double widthScale = targetImageData.getTargetWidth() / innerPage.getMediaBox().getWidth();
            double heightScale = targetImageData.getTargetHeight() / innerPage.getMediaBox().getHeight();

            try (PDDocument document = Loader.loadPDF(templateBytes)) {

                LayerUtility layerUtility = new LayerUtility(document);
                PDFormXObject innerPageAsForm = layerUtility.importPageAsForm(innerDocument, innerPage);

                AffineTransform affineTransform = new AffineTransform();
                affineTransform.translate(targetImageData.getHorizontalDisplacement(), targetImageData.getVerticalDisplacement());
                affineTransform.scale(widthScale, heightScale);
                PDPage destPage = document.getPage(0);

                layerUtility.wrapInSaveRestore(destPage);
                layerUtility.appendFormAsLayer(destPage, innerPageAsForm, affineTransform, headerSentence);

                pdfSignatureService.signAndSave(document, result);

            }

        } catch (Exception e) {
            log.error("Problem when printing attachment inside template of attachments");
            log.error(e.getMessage(), e.getCause());
        }
        return result;
    }

    private void addDocument(PDFMergerUtility ut, InputStream pdfDocument, List<Integer> indexPagesForDocuments, boolean newCategoryDocument,
            List<Tenant> tenantList, String headerSentence) {

        ByteArrayOutputStream templateWithTextsHeader = addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(tenantList, headerSentence,
                null);

        try (PDDocument innerDocument = Loader.loadPDF(pdfDocument.readAllBytes())) {

            for (PDPage innerPage : innerDocument.getPages()) {
                ByteArrayOutputStream pdfDocPageWithAttachmentMerged = mergePageInsideTemplate(innerDocument, innerPage, templateWithTextsHeader.toByteArray(),
                        headerSentence);
                ut.addSource(new RandomAccessReadBuffer(pdfDocPageWithAttachmentMerged.toByteArray()));
            }
            if (newCategoryDocument) {
                indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + innerDocument.getNumberOfPages());
            } else {
                indexPagesForDocuments.set(indexPagesForDocuments.size() - 1,
                        indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + innerDocument.getNumberOfPages());
            }
        } catch (Exception e) {
            log.error("Unable to addDocument - headerSentence:" + headerSentence, e);
        }

    }

    private void addDocumentOfClarification(PDFMergerUtility ut, List<Tenant> tenantList, Tenant mainTenant, List<Integer> indexPagesForDocuments,
            PDOutlineItem pdOutlineItem) {
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

            ByteArrayOutputStream outputStream = addTextHeaderAndTextBodyToTheCopyOfAttachmentsAndClarificationTemplate(tenantList, LE_MOT_DU_LOCATAIRE,
                    mainTenant.getClarification());
            ut.addSource(new RandomAccessReadBuffer(outputStream.toByteArray()));
            indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1) + 1);
        } else {
            indexPagesForDocuments.add(indexPagesForDocuments.get(indexPagesForDocuments.size() - 1)); // does not exist - stay on current page
        }
    }

    private void addFilesOfDocumentsToDossierPDF(PDFMergerUtility ut, List<Tenant> tenantList, List<Integer> indexPagesForDocuments,
            PDOutlineItem pdOutlineItem) {
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

            List<Guarantor> guarantorsOrderedByType = tenant1.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor))
                    .collect(Collectors.toList());
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
                    String sentence = getSentenceForGuarantorFromDocumentCategory(counterOfGuarantor, guarantor1.getTypeGuarantor(), currentCategory,
                            tenant1.getFirstName(), counter);
                    addDocument(ut, documentInputStream, indexPagesForDocuments, firstDocumentSubject || previousCategory != currentCategory, tenantList,
                            sentence);
                    firstDocumentSubject = false;
                    previousCategory = currentCategory;

                }
                if (guarantorsOrderedByType.size() > 1) {
                    counterOfGuarantor++;
                }
            }
        }
    }

    private void checkAllTenantsCompletedOrValidatedAndAllDocumentsNotNull(long apartmentSharingId, Long id) {
        int numberOfTenants = tenantRepository.countTenantsBlockingFullPdfGeneration(apartmentSharingId);
        if (numberOfTenants > 0) {
            throw new ApartmentSharingUnexpectedException(id);
        }
    }

    @Override
    public InputStream render(ApartmentSharing apartmentSharing) throws IOException {

        checkAllTenantsCompletedOrValidatedAndAllDocumentsNotNull(apartmentSharing.getId(), apartmentSharing.getId());

        if (apartmentSharing.getStatus() == TenantFileStatus.VALIDATED) {
            return validatedDossierPdfRenderer.render(apartmentSharing);
        }

        PDFMergerUtility ut = new PDFMergerUtility();
        List<Tenant> tenantList = apartmentSharing.getTenants().stream().sorted(Comparator.comparing(Tenant::getTenantType)).collect(Collectors.toList());

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

        // Only non verified dossiers, TO_PROCESS or COMPLETED, reach this code. They have no index pages, so the content
        // starts at page 0. Their pages, the clarification page included, do not show the RF and DossierFacile logos.
        indexPagesForDocuments.add(0);

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
        ut.setDocumentMergeMode(DocumentMergeMode.OPTIMIZE_RESOURCES_MODE);

        try {
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly().streamCache);
        } catch (IOException e) {
            log.error("Problem merge document for pdf full");
            log.error(e.getMessage(), e.getCause());
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (PDDocument doc = Loader.loadPDF(merge.toByteArray())) {

            doc.getDocumentCatalog().setDocumentOutline(pdDocumentOutline);
            pdOutlineItem.openNode();
            pdDocumentOutline.openNode();

            addPaginate(doc);
            doc.save(result);
            log.info("Generation completed");
        } catch (IOException e) {
            log.error("Problem creating full pdf", e);
            throw e;
        }
        // optimisation
        try (ByteArrayOutputStream finalResult = new ByteArrayOutputStream();
                PDDocument originDocument = Loader.loadPDF(result.toByteArray())) {

            new PdfOptimizer().optimize(originDocument);
            pdfSignatureService.signAndSave(originDocument, finalResult);

            return new ByteArrayInputStream(finalResult.toByteArray());
        } catch (Exception e) {
            log.warn("Optimisation FAILED !", e);
        }
        return new ByteArrayInputStream(result.toByteArray());

    }
}
