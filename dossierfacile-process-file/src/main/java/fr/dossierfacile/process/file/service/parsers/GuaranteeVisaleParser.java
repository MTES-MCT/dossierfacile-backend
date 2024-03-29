package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(3)
public class GuaranteeVisaleParser extends AbstractImagesParser<GuaranteeProviderFile> implements FileParser<GuaranteeProviderFile> {

    @Override
    protected String getJsonModelFile() {
        return "/parsers/visale.json";
    }

    @Override
    protected GuaranteeProviderFile getResultFromExtraction(Map<String, String> extractedTexts) {
        String zoneIdentification = extractedTexts.get("namesZones");
        String[] zonesId = zoneIdentification.split("\n");
        String[] firstNames = zonesId[0].split("Pr[éeè]nom[:\\s\\d]+");
        String[] lastNames = zonesId[1].split("Nom[:\\s\\d]+");
        List<GuaranteeProviderFile.FullName> fullNames = new LinkedList<>();
        for (int i = 1; i < firstNames.length && i < lastNames.length; i++) {
            fullNames.add(new GuaranteeProviderFile.FullName(firstNames[i].trim(), lastNames[i].trim()));
        }
        if (fullNames.isEmpty()) {
            log.error("Firstname/Lastname not found");
            return null;
        }
        if (StringUtils.isBlank(extractedTexts.get("visaNumber"))) {
            log.error("visaNumber is not found");
            return null;
        }
        if (StringUtils.isBlank(extractedTexts.get("deliveryDate"))) {
            log.error("deliveryDate is not found");
            return null;
        }
        if (StringUtils.isBlank(extractedTexts.get("validityDate"))) {
            log.error("validityDate is not found");
            return null;
        }

        return GuaranteeProviderFile.builder()
                .names(fullNames)
                .visaNumber(extractedTexts.get("visaNumber").trim())
                .validityDate(extractedTexts.get("deliveryDate").trim())
                .validityDate(extractedTexts.get("validityDate").trim())
                .build();
    }

    @Override
    public boolean shouldTryToApply(File file) {
        return (file.getDocument().getDocumentCategory() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE
                && file.getDocument().getDocumentSubCategory() == VISALE
                && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getStorageFile().getContentType()));
    }
}
