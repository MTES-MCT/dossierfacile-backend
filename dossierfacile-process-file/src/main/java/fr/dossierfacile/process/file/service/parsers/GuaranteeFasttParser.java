package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import fr.dossierfacile.process.file.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class GuaranteeFasttParser extends AbstractPDFParser<GuaranteeProviderFile> implements FileParser<GuaranteeProviderFile> {

    @Override
    protected String getJsonModelFile() {
        return "/parsers/fasttN1ZT.json";
    }

    @Override
    protected GuaranteeProviderFile getResultFromExtraction(PDFTextStripperByArea stripper, int page, GuaranteeProviderFile result) {
        return GuaranteeProviderFile.builder()
                .names(List.of(new GuaranteeProviderFile.FullName(
                        stripper.getTextForRegion("firstName").trim(),
                        stripper.getTextForRegion("lastName").trim()
                )))
                .visaNumber(stripper.getTextForRegion("visaNumber").trim())
                .validityDate(stripper.getTextForRegion("validityDate").trim())
                .build();
    }

    @Override
    protected boolean modelMatches(PageExtractorModel model, PDPage page, int pageNumber) throws IOException {
        if (!super.modelMatches(model, page, pageNumber)) {
            return false;
        }
        PDResources resources = page.getResources();
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);
            if (xObject instanceof PDImageXObject xImage) {
                BufferedImage image = xImage.getImage();
                String md5 = ImageUtils.md5(image);
                boolean result = model.getBackgroundImageMD5().equals(md5);
                if (!result) {
                    log.debug("MD5 mismatches " + md5);
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE
                && file.getDocument().getDocumentSubCategory() == OTHER_GUARANTEE
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}
