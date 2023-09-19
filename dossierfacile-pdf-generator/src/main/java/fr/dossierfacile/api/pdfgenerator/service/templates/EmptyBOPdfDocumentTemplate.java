package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.repository.GuarantorRepository;
import fr.dossierfacile.api.pdfgenerator.repository.TenantRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import fr.dossierfacile.api.pdfgenerator.util.Utility;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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

@Service
@AllArgsConstructor
@Slf4j
public class EmptyBOPdfDocumentTemplate implements PdfTemplate<Document> {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    public static final ClassPathResource ALTERNATIVE_FONT = new ClassPathResource("static/fonts/Noto/NotoEmoji-Medium.ttf");

    private final Locale locale = LocaleContextHolder.getLocale();
    private final MessageSource messageSource;
    private final TenantRepository tenantRepository;
    private final GuarantorRepository guarantorRepository;

    private ByteArrayOutputStream createPdfFromTemplate(Document document) throws IOException {
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

        try (PDDocument pdDocument = PDDocument.load(pdfTemplate.getInputStream())) {

            PDType0Font font = PDType0Font.load(pdDocument, new ClassPathResource("static/fonts/ArialNova-Light.ttf").getInputStream());
            PDType0Font alternativeFont = PDType0Font.load(pdDocument, ALTERNATIVE_FONT.getInputStream());
            PDPage pdPage = pdDocument.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, true, true);
            contentStream.setNonStrokingColor(74 / 255.0F, 144 / 255.0F, 226 / 255.0F);
            float fontSize = 11;
            float leading = 1.5f * fontSize;
            contentStream.setFont(font, fontSize);
            float marginY = 360;
            float marginX = 60;
            PDRectangle mediaBox = pdPage.getMediaBox();
            float width = mediaBox.getWidth() - 2 * marginX;
            float startX = mediaBox.getLowerLeftX() + marginX;
            float startY = mediaBox.getUpperRightY() - marginY;

            Optional<Tenant> tenantOptional = tenantRepository.getTenantByDocumentId(document.getId());
            if (tenantOptional.isEmpty()) {
                guarantorRepository.getGuarantorByDocumentId(document.getId()).ifPresent(
                        guarantor -> {
                            String fullNameGuarantor = String.join(" ",
                                    guarantor.getFirstName() != null ? guarantor.getFirstName() : "",
                                    guarantor.getLastName() != null ? guarantor.getLastName() : "");
                            textToShowInPdf.set(0, StringUtils.normalizeSpace(
                                    StringUtils.replace(fullNameGuarantor, "�", "_")
                                            + " " + textToShowInPdf.get(0)));
                            textToShowInPdf.set(1, StringUtils.normalizeSpace(textToShowInPdf.get(1)));
                        }
                );
            } else {
                Tenant tenant = tenantOptional.get();
                textToShowInPdf.set(0, StringUtils.normalizeSpace(
                        StringUtils.replace(tenant.getFullName(), "�", "_")
                                + " " + textToShowInPdf.get(0)));
                textToShowInPdf.set(1, StringUtils.normalizeSpace(textToShowInPdf.get(1)));
            }

            contentStream.setLeading(leading);

            for (String s : textToShowInPdf) {
                Utility.addText(contentStream, width, startX, startY, s, font, fontSize, alternativeFont);
                startY -= 36;
            }

            contentStream.close();
            pdDocument.save(outputStream);

        } catch (IOException e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            throw e;
        }
        return outputStream;
    }

    @Override
    public InputStream render(Document document) throws IOException {
        return new ByteArrayInputStream(createPdfFromTemplate(document).toByteArray());
    }
}
