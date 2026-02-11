package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

public abstract class BaseTaxRule extends BaseDocumentIAValidator {

    protected boolean isTaxOrDeclarativeSituation(BarcodeModel barcodeModel) {
        return isTax(barcodeModel) || isDeclarativeSituation(barcodeModel);
    }

    protected boolean isTax(BarcodeModel barcodeModel) {
        return barcodeModel.getAntsType() != null && (barcodeModel.getAntsType().equals("avis_imposition") || barcodeModel.getAntsType().equals("avis_imposition_v1"));
    }

    protected boolean isDeclarativeSituation(BarcodeModel barcodeModel) {
        if (barcodeModel.getAntsType() != null) {
            return false;
        }
        if (barcodeModel.getRawData() instanceof java.util.Map) {
            java.util.Map<?, ?> rawData = (java.util.Map<?, ?>) barcodeModel.getRawData();
            Object docType = rawData.get("doc_type");
            return "27".equals(docType) || "24".equals(docType) || "18".equals(docType);
        }
        return false;
    }

}
