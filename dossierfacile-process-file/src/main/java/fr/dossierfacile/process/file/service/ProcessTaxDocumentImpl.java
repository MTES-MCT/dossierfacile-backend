package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiMonFranceConnect;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.util.Utility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTaxDocumentImpl implements ProcessTaxDocument {
    private static final String URL_DELIMITER = "/";
    private final ApiParticulier apiParticulier;
    private final Utility utility;
    private final ApiTesseract apiTesseract;
    private final ApiMonFranceConnect apiMonFranceConnect;
    @Value("${tesseract.api.ocr.dpi.tax}")
    private int tesseractApiOcrDpiTax;
    @Value("${application.domain}")
    private String applicationDomain;
    @Value("${application.file.path}")
    private String applicationFilePath;

    @Override
    public TaxDocument process(Document document, Tenant tenant) {
        log.info("Starting with process of tax document");
        List<File> files = Optional.ofNullable(document.getFiles()).orElse(new ArrayList<>());
        TaxDocument taxDocument = processTaxDocumentWithQRCode(files);

        if (taxDocument.getQrContent() == null) {
            taxDocument = processTaxDocumentWithoutQRCode(files, tenant.getLastName(), tenant.getFirstName(),
                    Utility.normalize(tenant.getFirstName()), Utility.normalize(tenant.getLastName()));
        }

        log.info("Finishing with process of tax document");
        return taxDocument;
    }

    private TaxDocument processTaxDocumentWithQRCode(List<File> files) {
        long time = System.currentTimeMillis();
        log.info("Extracting QR content from tax document");

        TaxDocument taxDocument = new TaxDocument();

        StringBuilder currentQrContent = new StringBuilder();
        List<File> pdfs = files.stream().filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf")).collect(Collectors.toList());
        if (!pdfs.isEmpty()) {
            for (File pdf : pdfs) {
                String url = utility.extractQRCodeInfo(pdf);
                if (url != null && !url.isBlank()) {
                    ResponseEntity<List> response = apiMonFranceConnect.monFranceConnect(url);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        log.info("Api MonFranceConnect Response {}", response.getStatusCodeValue());

                        checkIfInfoBehindQrContentMatchesPdfContent(pdf, response, taxDocument);

                        String bodyResponse = Objects.requireNonNull(response.getBody()).toString();
                        if (!currentQrContent.toString().isBlank()) {
                            currentQrContent.append(", ").append(bodyResponse);
                        } else {
                            currentQrContent = new StringBuilder(bodyResponse);
                        }
                    } else {
                        log.warn("Api MonFranceConnect Response {}", response.getStatusCodeValue());
                    }
                }
            }
        }
        if (!currentQrContent.toString().isBlank()) {
            taxDocument.setQrContent(currentQrContent.toString());
        }
        log.info("Extracted QR content : {}", currentQrContent);
        long milliseconds = System.currentTimeMillis() - time;
        log.info("Finishing with extraction of QR content of document in {} ms", milliseconds);
        taxDocument.setTime(milliseconds);
        return taxDocument;
    }

    private TaxDocument processTaxDocumentWithoutQRCode(List<File> files, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        long time = System.currentTimeMillis();
        log.info("Processing tax document without QR code");

        boolean test1 = false;
        boolean test2 = false;
        TaxDocument taxDocument = new TaxDocument();

        StringBuilder result = new StringBuilder();
        List<File> pdfs = files.stream().filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf")).collect(Collectors.toList());
        if (!pdfs.isEmpty()) {
            for (File pdf : pdfs) {
                result.append(utility.extractInfoFromPDFFirstPage(pdf));
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

        log.info("Finishing processing tax document without QR code");
        long milliseconds = System.currentTimeMillis() - time;
        taxDocument.setTime(milliseconds);
        return taxDocument;
    }

    private void checkIfInfoBehindQrContentMatchesPdfContent(File pdf, ResponseEntity<List> response, TaxDocument taxDocument) {
        List<String> listResponse = Objects.requireNonNull(response.getBody());

        String result = utility.extractInfoFromPDFFirstPage(pdf);
        AtomicInteger i = new AtomicInteger();
        if (!listResponse.isEmpty()) {
            listResponse.forEach(element -> {
                if (result.contains(element)) {
                    i.getAndIncrement();
                }
            });
            if (listResponse.size() == i.get()) {
                log.info("QR content VALID for PDF with ID [" + pdf.getId() + "]");
                taxDocument.setTaxContentValid(Boolean.TRUE);
            } else {
                String path = applicationDomain + URL_DELIMITER + applicationFilePath + URL_DELIMITER + pdf.getPath();
                String tesseractResult = apiTesseract.apiTesseract(path, new int[]{1}, tesseractApiOcrDpiTax);

                AtomicInteger ii = new AtomicInteger();
                listResponse.forEach(element -> {
                    if (tesseractResult.contains(element)) {
                        ii.getAndIncrement();
                    }
                });
                if (listResponse.size() == ii.get()) {
                    log.info("QR content VALID for PDF with ID [" + pdf.getId() + "]");
                    taxDocument.setTaxContentValid(Boolean.TRUE);
                } else {
                    taxDocument.setTaxContentValid(Boolean.FALSE);
                    log.warn("QR content NOT VALID for the PDF with ID [" + pdf.getId() + "]");
                }
            }
        }
    }

    //check if the name is OK
    private boolean test1(Taxes taxes, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
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


