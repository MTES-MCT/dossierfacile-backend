package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.amqp.Producer;
import fr.dossierfacile.api.pdfgenerator.repository.DocumentRepository;
import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.api.pdfgenerator.util.PdfGenerator;
import fr.dossierfacile.api.pdfgenerator.util.PdfMergeModel;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.OvhService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.UnexpectedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private static final String FAILED_PDF_GENERATION = "Failed PDF Generation. ";
    private static final String NUMBER_OF_RETRIES_EXHAUSTED = "Number of PDF generation retries exhausted";

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final OvhService ovhService;
    private final Producer producer;

    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;

    @Value("${pdf.generation.reattempts}")
    private Integer maxRetries;

    private static void addParagraph(PDPageContentStream contentStream, float width, float sx,
                                     float sy, List<String> text, boolean justify, PDFont font, float fontSize, float leading) throws IOException {
        List<List<String>> listList = parseLines(text, width, font, fontSize);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(sx, sy);
        for (List<String> lines : listList
        ) {
            for (String line : lines) {
                float charSpacing = 0;
                if (justify && line.length() > 1) {
                    float size = fontSize * font.getStringWidth(line) / 1000;
                    float free = width - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }

                }
                contentStream.setCharacterSpacing(charSpacing);
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, leading);
            }
        }
    }

    private static List<List<String>> parseLines(List<String> list, float width, PDFont font, float fontSize) throws IOException {
        List<List<String>> listArrayList = new ArrayList<>();
        for (String text : list
        ) {
            List<String> lines = new ArrayList<>();
            int lastSpace = -1;
            while (text.length() > 0) {
                int spaceIndex = text.indexOf(' ', lastSpace + 1);
                if (spaceIndex < 0)
                    spaceIndex = text.length();
                String subString = text.substring(0, spaceIndex);
                float size = fontSize * font.getStringWidth(subString) / 1000;
                if (size > width) {
                    if (lastSpace < 0) {
                        lastSpace = spaceIndex;
                    }
                    subString = text.substring(0, lastSpace);
                    lines.add(subString);
                    text = text.substring(lastSpace).trim();
                    lastSpace = -1;
                } else if (spaceIndex == text.length()) {
                    lines.add(text);
                    text = "";
                } else {
                    lastSpace = spaceIndex;
                }
            }
            listArrayList.add(lines);
        }
        return listArrayList;
    }

    @Transactional
    public void processPdfGenerationOfDocument(Long documentId) {
        String processIdentifier = "pdf-process-" + UUID.randomUUID();
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn(FAILED_PDF_GENERATION + "Document with ID [" + documentId + "] doesn't exist anymore in the DB");
        } else {
            lockDocument(document, processIdentifier);
            if (document.getRetries() + 1 > maxRetries) {
                log.warn(FAILED_PDF_GENERATION + NUMBER_OF_RETRIES_EXHAUSTED);
            } else if (!document.getLockedBy().equals(processIdentifier)) {
                log.warn(FAILED_PDF_GENERATION + "Document with ID [" + documentId + "] is being generated by another process");
            } else {
                //New attempt
                generatePdf(document);
            }
        }
    }

    @Override
    @Transactional
    public void lockDocument(Document document, String lockedBy) {
        if (!document.isLocked()) {
            document.setLocked(true);
            document.setLockedBy(lockedBy);
            documentRepository.save(document);
        }
    }

    @Override
    public void unLockDocumentSuccessfulGeneration(Document document) {
        document.setLocked(false);
        document.setLockedBy(null);
        document.setRetries(0);
    }

    @Override
    public void unLockDocumentFailedGeneration(Document document) {
        document.setLocked(false);
        document.setLockedBy(null);
    }

    private void incrementRetries(Document document) {
        document.setRetries(document.getRetries() + 1);
    }

    @Override
    @Transactional
    public void generatePdf(Document document) {
        long documentId = document.getId();
        DocumentCategory documentCategory = document.getDocumentCategory();
        DocumentSubCategory documentSubCategory = document.getDocumentSubCategory();
        String documentCategoryName = documentCategory.name();
        log.info("Generating PDF for document [" + documentCategoryName + "] with ID [" + documentId + "]");
        List<PdfMergeModel> pdfMergeModels = new ArrayList<>();

        if (documentSubCategory == DocumentSubCategory.MY_PARENTS
                || documentSubCategory == DocumentSubCategory.LESS_THAN_YEAR
                || ((documentSubCategory == DocumentSubCategory.OTHER_TAX
                || documentCategory == DocumentCategory.FINANCIAL)
                && document.getNoDocument())) {
            InputStream fileIS = new ByteArrayInputStream(createPdfFromTemplate(document).toByteArray());
            pdfMergeModels.add(PdfMergeModel
                    .builder()
                    .extension("pdf")
                    .applyWatermark(false)
                    .inputStream(fileIS)
                    .build());
        } else {
            List<String> pathFiles = fileRepository.getFilePathsByDocumentId(document.getId());
            if (pathFiles != null && !pathFiles.isEmpty()) {
                for (String path : pathFiles) {
                    SwiftObject swiftObject = ovhService.get(path);
                    if (swiftObject != null) {
                        DLPayload dlPayload = swiftObject.download();
                        if (dlPayload.getHttpResponse().getStatus() == HttpStatus.OK.value()) {
                            log.info("File with path/name [" + path + "] downloaded successfully, for document [" + documentCategoryName + "] with ID [" + documentId + "]");
                            String extension = FilenameUtils.getExtension(path).toLowerCase(Locale.ROOT);
                            if (!extension.equals("pdf") && !extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                                String exceptionMessage = "Unexpected extension for file with path [" + path + "]";
                                log.error(exceptionMessage + ". It will not be added to the pdf of document [" + documentCategoryName + "] with ID [" + documentId + "]");
                                UnexpectedException exception = new UnexpectedException(exceptionMessage);
                                log.error(EXCEPTION + Sentry.captureException(exception));
                            } else {
                                pdfMergeModels.add(PdfMergeModel
                                        .builder()
                                        .extension(extension)
                                        .inputStream(dlPayload.getInputStream())
                                        .build());
                            }
                            continue;
                        }
                    }
                    log.warn("Problem downloading file with path/name [" + path + "]. It will not be added to the pdf of document [" + documentCategoryName + "] with ID [" + documentId + "]");
                }
            } else {
                log.error("No file were found in the database for generate document [" + documentCategoryName + "] with ID [" + documentId + "]");
            }
        }

        if (!pdfMergeModels.isEmpty()) {
            String name = document.getName() == null || document.getName().isBlank() ? UUID.randomUUID() + ".pdf" : document.getName();
            ovhService.upload(name, new ByteArrayInputStream(PdfGenerator.generatePdf(pdfMergeModels)));
            document.setName(name);
            document.setProcessingEndTime(LocalDateTime.now());

            unLockDocumentSuccessfulGeneration(document);
            documentRepository.save(document);
            log.info("Sucessful PDF generation. Document [" + documentCategoryName + "] with ID [" + documentId + "]");
        } else {

            unLockDocumentFailedGeneration(document);
            if (document.getRetries() + 1 < maxRetries) {
                producer.generatePdf(documentId);
            }
            incrementRetries(document);
            documentRepository.save(document);
            log.error("Failed PDF generation. Document [" + documentCategoryName + "] with ID [" + documentId + "]");
        }
    }

    private ByteArrayOutputStream createPdfFromTemplate(Document document) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Resource pdfTemplate;
        List<String> textToShowInPdf = new ArrayList<>();
        if (document.getDocumentCategory() == DocumentCategory.FINANCIAL) {
            pdfTemplate = new ClassPathResource("static/pdf/template_document_financial.pdf");
            textToShowInPdf.add(0, messageSource.getMessage("tenant.document.financial.justification.nodocument", null, locale));
            textToShowInPdf.add(1, document.getCustomText());
        } else { //DocumentCategory.TAX
            pdfTemplate = new ClassPathResource("static/pdf/template_document_tax.pdf");
            textToShowInPdf.add(0, messageSource.getMessage("tenant.document.tax.justification.nodocument", null, locale));
            if (document.getDocumentSubCategory() == DocumentSubCategory.MY_PARENTS) {
                textToShowInPdf.add(1, messageSource.getMessage("tenant.document.tax.justification.parents", null, locale));
            } else if (document.getDocumentSubCategory() == DocumentSubCategory.LESS_THAN_YEAR) {
                textToShowInPdf.add(1, messageSource.getMessage("tenant.document.tax.justification.less_than_year", null, locale));
            } else { //DocumentSubCategory.OTHER_TAX
                textToShowInPdf.add(1, document.getCustomText());
            }
        }

        try {
            PDDocument pdDocument = PDDocument.load(pdfTemplate.getInputStream());
            PDType0Font font = PDType0Font.load(pdDocument, new ClassPathResource("static/fonts/ArialNova-Light.ttf").getInputStream());
            PDPage pdPage = pdDocument.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, true, true);
            contentStream.setNonStrokingColor(74 / 255.0F, 144 / 255.0F, 226 / 255.0F);
            float fontSize = 12;
            float leading = -1.5f * fontSize;
            contentStream.setFont(font, fontSize);
            float marginY = 490;
            float marginX = 60;
            PDRectangle mediaBox = pdPage.getMediaBox();
            float width = mediaBox.getWidth() - 2 * marginX;
            float startX = mediaBox.getLowerLeftX() + marginX;
            float startY = mediaBox.getUpperRightY() - marginY;
            contentStream.beginText();

            Tenant tenant = document.getTenant();
            if (tenant == null) {
                Guarantor guarantor = document.getGuarantor();
                String fullNameGuarantor = String.join(" ",
                        guarantor.getFirstName() != null ? guarantor.getFirstName() : "",
                        guarantor.getLastName() != null ? guarantor.getLastName() : "");
                textToShowInPdf.set(0, StringUtils.normalizeSpace(
                        StringUtils.replace(fullNameGuarantor, "�", "_")
                                + " " + textToShowInPdf.get(0)));
                textToShowInPdf.set(1, StringUtils.normalizeSpace(textToShowInPdf.get(1)));
            } else {
                textToShowInPdf.set(0, StringUtils.normalizeSpace(
                        StringUtils.replace(tenant.getFullName(), "�", "_")
                                + " " + textToShowInPdf.get(0)));
                textToShowInPdf.set(1, StringUtils.normalizeSpace(textToShowInPdf.get(1)));
            }

            addParagraph(
                    contentStream,
                    width,
                    startX,
                    startY,
                    textToShowInPdf,
                    true,
                    font,
                    fontSize,
                    leading
            );
            contentStream.endText();
            contentStream.close();
            pdDocument.save(outputStream);
            pdDocument.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
            log.error(EXCEPTION + Sentry.captureException(e));
        }
        return outputStream;
    }
}
