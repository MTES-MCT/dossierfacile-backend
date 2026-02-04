package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

public abstract class BaseTaxRule extends BaseDocumentIAValidator {

    protected boolean isTaxOrDeclarativeSituation(BarcodeModel barcodeModel) {
        return isTax(barcodeModel) || isDeclarativeSituation(barcodeModel);
    }

    protected boolean isTax(BarcodeModel barcodeModel) {
        return barcodeModel.getAntsType() != null && barcodeModel.getAntsType().equals("avis_imposition");
    }

    protected boolean isDeclarativeSituation(BarcodeModel barcodeModel) {
        if (barcodeModel.getAntsType() != null) {
            return false;
        }
        if (barcodeModel.getRawData() instanceof java.util.Map) {
            java.util.Map<?, ?> rawData = (java.util.Map<?, ?>) barcodeModel.getRawData();
            return "27".equals(rawData.get("doc_type"));
        }
        return false;
    }

}
