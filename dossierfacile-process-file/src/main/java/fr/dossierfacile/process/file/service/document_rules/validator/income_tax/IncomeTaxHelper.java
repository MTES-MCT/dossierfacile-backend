package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IncomeTaxHelper {

    private IncomeTaxHelper() {
        // Utility class, no instantiation needed
    }

    public static Optional<TaxIncomeMainFile> fromQR(BarCodeFileAnalysis barCodeFileAnalysis) {
        if (barCodeFileAnalysis.getBarCodeType() != BarCodeType.TWO_D_DOC) {
            return Optional.empty();
        }
        Map<String, String> dataWithLabel = new ObjectMapper().convertValue(barCodeFileAnalysis.getVerifiedData(), Map.class);
        return Optional.of(TaxIncomeMainFile.builder()
                .declarant1NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .declarant1Nom(dataWithLabel.get(TwoDDocDataType.ID_46.getLabel()))
                .declarant2NumFiscal(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .declarant2Nom(dataWithLabel.get(TwoDDocDataType.ID_48.getLabel()))
                .anneeDesRevenus(Integer.parseInt(dataWithLabel.get(TwoDDocDataType.ID_45.getLabel())))
                .nombreDeParts(dataWithLabel.get(TwoDDocDataType.ID_43.getLabel()))
                .dateDeMiseEnRecouvrement(dataWithLabel.get(TwoDDocDataType.ID_4A.getLabel()))
                .revenuFiscalDeReference(Integer.parseInt(dataWithLabel.get(TwoDDocDataType.ID_41.getLabel())))
                .numeroFiscalDeclarant1(dataWithLabel.get(TwoDDocDataType.ID_47.getLabel()))
                .numeroFiscalDeclarant2(dataWithLabel.get(TwoDDocDataType.ID_49.getLabel()))
                .referenceAvis(dataWithLabel.get(TwoDDocDataType.ID_44.getLabel())).build());
    }

    public static List<Integer> getProvidedYears(Document document) {
        List<Integer> providedYears = new ArrayList<>(2);
        for (File dfFile : document.getFiles()) {
            if (dfFile.getFileAnalysis() != null) {
                Optional<TaxIncomeMainFile> qrDocument = IncomeTaxHelper.fromQR(dfFile.getFileAnalysis());
                if (qrDocument.isEmpty()) {
                    continue;
                }
                var safeQrDocument = qrDocument.get();
                if (safeQrDocument.getAnneeDesRevenus() != null)
                    providedYears.add(safeQrDocument.getAnneeDesRevenus());
            }
        }
        return providedYears;
    }
}
