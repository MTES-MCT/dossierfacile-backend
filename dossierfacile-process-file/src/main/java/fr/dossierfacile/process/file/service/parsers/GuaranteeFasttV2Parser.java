package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile.FullName;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.parsers.tools.PageExtractorModel;
import fr.dossierfacile.process.file.util.ImageUtils;
import fr.dossierfacile.process.file.util.TextUtil;
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
@Order(2)
public class GuaranteeFasttV2Parser extends AbstractPDFParser<GuaranteeProviderFile> implements FileParser<GuaranteeProviderFile> {

    @Override
    protected String getJsonModelFile() {
        return "/parsers/fasttN2ZT.json";
    }

    @Override
    protected GuaranteeProviderFile getResultFromExtraction(PDFTextStripperByArea stripper, GuaranteeProviderFile previousResult) {

        return GuaranteeProviderFile.builder()
                .names(List.of(new FullName(
                                        TextUtil.cleanAndTrim(stripper.getTextForRegion("firstName")),
                                        TextUtil.cleanAndTrim(stripper.getTextForRegion("lastName"))),
                                new FullName(
                                        TextUtil.cleanAndTrim(stripper.getTextForRegion("firstName2")),
                                        TextUtil.cleanAndTrim(stripper.getTextForRegion("lastName2")))
                        )
                )
                .visaNumber(TextUtil.cleanAndTrim(stripper.getTextForRegion("visaNumber")))
                .validityDate(TextUtil.cleanAndTrim(stripper.getTextForRegion("validityDate")))
                .build();
    }

    // Currenlty FASTT PDF can be identified thanks to their background image
    @Override
    protected boolean modelMatches(PageExtractorModel model, PDPage page) throws IOException {
        if (!super.modelMatches(model, page)) {
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
