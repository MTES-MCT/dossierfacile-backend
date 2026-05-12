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
        if (barcodeModel.getDocType() == null)  {
            return false;
        }
        return barcodeModel.getDocType().equals("27") || barcodeModel.getDocType().equals("24") || barcodeModel.getDocType().equals("18");
    }

}
