package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.model.PdfMergeModel;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
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
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final AuthenticationFacade authenticationFacade;
    private final OvhService ovhService;
    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;

    @Override
    public void delete(Long id) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        Optional<Document> documentOptional = documentRepository.findByIdAssociatedToTenantId(id, tenant.getId());
        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();
            updateOthersDocumentsStatus(tenant);
            ovhService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
            documentRepository.delete(document);
        }
    }

    @Override
    public void generatePdfByFilesOfDocument(Document document) {
        log.info("Generating PDF for document with id [" + document.getId() + "]");
        List<PdfMergeModel> pdfMergeModels = new ArrayList<>();
        String name = document.getName() == null || document.getName().isBlank() ? UUID.randomUUID().toString() + ".pdf" : document.getName();

        if (document.getDocumentSubCategory() == DocumentSubCategory.MY_PARENTS
                || document.getDocumentSubCategory() == DocumentSubCategory.LESS_THAN_YEAR
                || ((document.getDocumentSubCategory() == DocumentSubCategory.OTHER_TAX
                || document.getDocumentCategory() == DocumentCategory.FINANCIAL)
                && document.getNoDocument())) {
            InputStream fileIS = new ByteArrayInputStream(createPdfFromTemplate(document).toByteArray());
            pdfMergeModels.add(PdfMergeModel
                    .builder()
                    .extension("pdf")
                    .inputStream(fileIS)
                    .build());
        } else {
            List<String> paths = fileRepository.getFilePathsByDocumentId(document.getId());
            if (paths != null && !paths.isEmpty()) {
                for (String path : paths) {
                    SwiftObject swiftObject = ovhService.get(path);
                    if (swiftObject != null) {
                        InputStream fileIS = swiftObject.download().getInputStream();
                        String extension = FilenameUtils.getExtension(path).toLowerCase(Locale.ROOT);
                        if (extension.equals("pdf")) {
                            Utility.mergeAndWatermarkPdf(fileIS, pdfMergeModels);
                        } else {
                            Utility.mergeAndWatermarkImage(fileIS, pdfMergeModels);
                        }
                    }
                }
            }
        }

        if (!pdfMergeModels.isEmpty()) {
            ovhService.upload(name, new ByteArrayInputStream(Utility.mergePdfModels(pdfMergeModels).toByteArray()));
            document.setName(name);
            documentRepository.save(document);
            log.info("PDF generated and uploaded");
        } else {
            log.info("PDF was not generated or uploaded");
        }
    }

    @Override
    public void updateOthersDocumentsStatus(Tenant tenant) {
        if (tenant.getStatus().equals(TenantFileStatus.VALIDATED)) {
            tenant.getDocuments().forEach(document -> {
                document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                documentRepository.save(document);
            });
            tenant.getGuarantors().forEach(guarantor ->
                    guarantor.getDocuments().forEach(document -> {
                                document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                                documentRepository.save(document);
                            }
                    )
            );
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
        }
        return outputStream;
    }

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

}
