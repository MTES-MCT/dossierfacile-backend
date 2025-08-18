package fr.dossierfacile.process.file.service.documentrules.validator.payslip;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import fr.dossierfacile.process.file.util.TwoDDocUtil;

import java.time.YearMonth;
import java.util.Map;

public class PayslipRuleCheckQRCode extends AbstractDocumentRuleValidator {

    private final boolean isFileWithQrCode;

    public PayslipRuleCheckQRCode(boolean isFileWithQrCode) {
        super();
        this.isFileWithQrCode = isFileWithQrCode;
    }


    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_PAYSLIP_QRCHECK;
    }

    @Override
    protected boolean isValid(Document document) {

        // This rule is only applied if the document has files with QR codes
        // Only applicable for public payslips
        if (!isFileWithQrCode) {
            return true;
        }

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || dfFile.getFileAnalysis() == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                continue;
            }
            if (analysis.getClassification() == ParsedFileClassification.PUBLIC_PAYSLIP) {
                PayslipFile qrDocument = fromQR(dfFile.getFileAnalysis());
                PayslipFile parsedDocument = (PayslipFile) analysis.getParsedFile();

                if (qrDocument == null
                        || qrDocument.getFullname() == null
                        || qrDocument.getMonth() == null
                        || qrDocument.getCumulativeNetTaxableIncome() == 0
                        || !PersonNameComparator.equalsWithNormalization(qrDocument.getFullname(), parsedDocument.getFullname())
                        || !qrDocument.getMonth().equals(parsedDocument.getMonth())
                        || Math.abs(qrDocument.getNetTaxableIncome() - parsedDocument.getNetTaxableIncome()) > 1
                        || Math.abs(qrDocument.getCumulativeNetTaxableIncome() - parsedDocument.getCumulativeNetTaxableIncome()) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private PayslipFile fromQR(BarCodeFileAnalysis barCodeFileAnalysis) {
        if (barCodeFileAnalysis.getBarCodeType() != BarCodeType.TWO_D_DOC) {
            throw new IllegalStateException("Verified Code has unsupported type");
        }
        Map<String, String> dataWithLabel = new ObjectMapper().convertValue(barCodeFileAnalysis.getVerifiedData(), Map.class);
        return PayslipFile.builder()
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .fullname(dataWithLabel.get(TwoDDocDataType.ID_10.getLabel()))
                .month(YearMonth.from(TwoDDocUtil.getLocalDateFrom2DDocHexDate(dataWithLabel.get(TwoDDocDataType.ID_54.getLabel()))))
                .netTaxableIncome(Double.parseDouble(dataWithLabel.get(TwoDDocDataType.ID_58.getLabel()).replace(" ", "").replace(',', '.')))
                .cumulativeNetTaxableIncome(Double.parseDouble(dataWithLabel.get(TwoDDocDataType.ID_59.getLabel()).replace(" ", "").replace(',', '.')))
                .build();
    }
}
