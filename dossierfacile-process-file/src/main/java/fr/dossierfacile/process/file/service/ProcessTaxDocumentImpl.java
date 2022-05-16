package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.util.Utility;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTaxDocumentImpl implements ProcessTaxDocument {
    private static final String URL_DELIMITER = "/";
    private final ApiParticulier apiParticulier;
    private final Utility utility;
    private final ApiTesseract apiTesseract;
    @Value("${tesseract.api.ocr.dpi.tax}")
    private int tesseractApiOcrDpiTax;
    @Value("${application.domain}")
    private String applicationDomain;
    @Value("${application.file.path}")
    private String applicationFilePath;

    @Override
    public TaxDocument process(Document document, Tenant tenant) {

        return processTaxDocument(Optional.ofNullable(document.getFiles()).orElse(new ArrayList<>()),
                tenant.getLastName(), tenant.getFirstName(),
                Utility.normalize(tenant.getFirstName()), Utility.normalize(tenant.getLastName())
        );
    }

    public TaxDocument processTaxDocument(List<File> files, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        long time = System.currentTimeMillis();
        log.info("Starting with process of tax document");

        boolean test1 = false;
        boolean test2 = false;
        TaxDocument taxDocument = new TaxDocument();

        StringBuilder result = new StringBuilder();
        List<File> pdfs = files.stream().filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf")).collect(Collectors.toList());
        if (!pdfs.isEmpty()) {
            for (File pdf : pdfs) {
                String[] info = utility.extractInfoFromPDFFirstPage(pdf.getPath());
                result.append(info[0]);
                taxDocument.setQrContent(info[1]);
            }
        }
        String fiscalNumber = Utility.extractFiscalNumber(result.toString());
        String referenceNumber = Utility.extractReferenceNumber(result.toString());

        if (fiscalNumber.equals("") || referenceNumber.equals("")) {
            result = new StringBuilder();
            List<String> paths = files.stream().map(file -> applicationDomain + URL_DELIMITER + applicationFilePath + URL_DELIMITER + file.getPath()).collect(Collectors.toList());
            if (!paths.isEmpty()) {
                for (String path : paths) {
                    result.append(apiTesseract.apiTesseract(path, new int[]{1}, tesseractApiOcrDpiTax));
                }
            }
            if (fiscalNumber.equals("")) {
                fiscalNumber = Utility.extractFiscalNumber(result.toString());
            }
            if (referenceNumber.equals("")) {
                referenceNumber = Utility.extractReferenceNumber(result.toString());
            }
        }

        if (!fiscalNumber.equals("") && !referenceNumber.equals("")) {
            log.info("Call to particulier api");
            ResponseEntity<Taxes> taxesResponseEntity = apiParticulier.particulierApi(fiscalNumber, referenceNumber);
            log.info("Response status {}", taxesResponseEntity.getStatusCodeValue());
            if (taxesResponseEntity.getStatusCode() == HttpStatus.OK) {
                test1 = test1(Objects.requireNonNull(taxesResponseEntity.getBody()), lastName, firstName, unaccentFirstName, unaccentLastName);
                test2 = test2(taxesResponseEntity.getBody(), result);
                taxDocument.setDeclarant1(taxesResponseEntity.getBody().getDeclarant1().toString());
                taxDocument.setDeclarant2(taxesResponseEntity.getBody().getDeclarant2().toString());
                taxDocument.setAnualSalary(taxesResponseEntity.getBody().getRevenuFiscalReference());
            }
        }

        taxDocument.setTest1(test1);
        taxDocument.setTest2(test2);
        taxDocument.setFiscalNumber(fiscalNumber.equals("") ? "fail" : fiscalNumber);
        taxDocument.setReferenceNumber(referenceNumber.equals("") ? "fail" : referenceNumber);

        log.info("Finishing with process of document");
        long milliseconds = System.currentTimeMillis() - time;
        taxDocument.setTime(milliseconds);
        return taxDocument;
    }

    //check if the name is OK
    public boolean test1(Taxes taxes, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        boolean result1 = (taxes.getDeclarant1() != null &&
                (StringUtils.containsIgnoreCase(taxes.getDeclarant1().getNom(), lastName) ||
                        StringUtils.containsIgnoreCase(taxes.getDeclarant1().getNomNaissance(), lastName)) &&
                StringUtils.containsIgnoreCase(taxes.getDeclarant1().getPrenoms(), firstName))

                || (taxes.getDeclarant2() != null &&
                (StringUtils.containsIgnoreCase(taxes.getDeclarant2().getNom(), lastName) ||
                        StringUtils.containsIgnoreCase(taxes.getDeclarant2().getNomNaissance(), lastName)) &&
                StringUtils.containsIgnoreCase(taxes.getDeclarant2().getPrenoms(), firstName));

        boolean result2 = (taxes.getDeclarant1() != null &&
                (StringUtils.containsIgnoreCase(taxes.getDeclarant1().getNom(), unaccentLastName) ||
                        StringUtils.containsIgnoreCase(taxes.getDeclarant1().getNomNaissance(), unaccentLastName)) &&
                StringUtils.containsIgnoreCase(taxes.getDeclarant1().getPrenoms(), unaccentFirstName))

                || (taxes.getDeclarant2() != null &&
                ((StringUtils.containsIgnoreCase(taxes.getDeclarant2().getNom(), unaccentLastName) ||
                        StringUtils.containsIgnoreCase(taxes.getDeclarant2().getNomNaissance(), unaccentLastName)) &&
                        StringUtils.containsIgnoreCase(taxes.getDeclarant2().getPrenoms(), unaccentFirstName)));

        return result1 || result2;
    }

    //check if the amount "montant fiscal de référence" is equals to 12*salary with an acceptable error of 10% (value in parameter)
    private boolean test2(Taxes taxes, StringBuilder stringBuilder) {
        Map<String, Integer> map = Utility.extractNumbersText(stringBuilder.toString());
        int salaryApi = taxes.getRevenuFiscalReference();
        return map.containsKey(String.valueOf(salaryApi));
    }
}


