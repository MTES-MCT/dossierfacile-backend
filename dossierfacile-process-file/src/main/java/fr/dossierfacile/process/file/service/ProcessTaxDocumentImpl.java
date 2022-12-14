package fr.dossierfacile.process.file.service;

import com.google.common.annotations.VisibleForTesting;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TaxFileExtractionType;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.model.TwoDDoc;
import fr.dossierfacile.process.file.service.interfaces.ApiMonFranceConnect;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessTaxDocumentImpl implements ProcessTaxDocument {
    private static final String URL_DELIMITER = "/";
    private final ApiParticulier apiParticulier;
    private final Utility utility;
    private final ApiTesseract apiTesseract;
    private final ApiMonFranceConnect apiMonFranceConnect;

    @Value("${application.domain}")
    private String applicationDomain;
    @Value("${application.file.path}")
    private String applicationFilePath;

    @Value("${feature.toggle.new.api}")
    private boolean newApi;

    @Override
    public TaxDocument process(Document document, Guarantor guarantor) {
        log.info("Starting with process of guarantor tax document");
        List<File> files = Optional.ofNullable(document.getFiles()).orElse(new ArrayList<>());
        TaxDocument taxDocument = processTaxDocumentWithQRCode(files);

        if (taxDocument.getQrContent() == null) {
            taxDocument = processTaxDocumentWith2DCode(files, guarantor.getLastName(), guarantor.getFirstName(),
                    Utility.normalize(guarantor.getFirstName()), Utility.normalize(guarantor.getLastName()));
        }

        if (taxDocument.getQrContent() == null) {
            taxDocument = processTaxDocumentWithOCR(files, guarantor.getLastName(), guarantor.getFirstName(),
                    Utility.normalize(guarantor.getFirstName()), Utility.normalize(guarantor.getLastName()));
        }

        log.info("Finishing with process of guarantor tax document");
        return taxDocument;
    }

    @Override
    public TaxDocument process(Document document, Tenant tenant) {
        if (!Boolean.TRUE.equals(tenant.getAllowCheckTax())
                || CollectionUtils.isEmpty(document.getFiles())) {
            TaxDocument doc = new TaxDocument();
            doc.setFileExtractionType(TaxFileExtractionType.NONE);
            return doc;
        }
        if (existingResultRetrievedViaFranceConnect(document)) {
            return document.getTaxProcessResult();
        }

        log.info("Starting with process of tax document");
        List<File> files = document.getFiles();
        TaxDocument taxDocument = processTaxDocumentWithQRCode(files);

        if (taxDocument.getQrContent() == null) {
            taxDocument = processTaxDocumentWith2DCode(files, tenant.getLastName(), tenant.getFirstName(),
                    Utility.normalize(tenant.getFirstName()), Utility.normalize(tenant.getLastName()));
        }

        if (taxDocument.getQrContent() == null) {
            taxDocument = processTaxDocumentWithOCR(files, tenant.getLastName(), tenant.getFirstName(),
                    Utility.normalize(tenant.getFirstName()), Utility.normalize(tenant.getLastName()));
        }

        log.info("Finishing with process of tax document");
        return taxDocument;
    }

    private boolean existingResultRetrievedViaFranceConnect(Document document) {
        TaxDocument taxProcessResult = document.getTaxProcessResult();
        return taxProcessResult != null &&
                taxProcessResult.getFileExtractionType() == TaxFileExtractionType.FRANCE_CONNECT;
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
                    currentQrContent = getMonFranceConnectCodeContent(taxDocument, currentQrContent, pdf, url);
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
        taxDocument.setFileExtractionType(TaxFileExtractionType.MON_FRANCE_CONNECT);
        return taxDocument;
    }

    @VisibleForTesting
    protected TaxDocument processTaxDocumentWith2DCode(List<File> files, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        long time = System.currentTimeMillis();
        log.info("Extracting 2D-doc content from tax document");

        TaxDocument taxDocument = new TaxDocument();

        List<File> pdfs = files.stream().filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf")).collect(Collectors.toList());
        if (!pdfs.isEmpty()) {
            for (File pdf : pdfs) {
                String twoDDocContent = utility.extractTax2DDoc(pdf);
                if (StringUtils.isNotBlank(twoDDocContent)) {
                    StringBuilder result = new StringBuilder(utility.extractInfoFromPDFFirstPage(pdf));
                    taxDocument = getTaxApiCodeContent(twoDDocContent, lastName, firstName, unaccentFirstName, unaccentLastName, result);
                    taxDocument.setQrContent(twoDDocContent);
                }
            }
        }

        log.info("Extracted 2D-doc content : {}", taxDocument.getQrContent());
        long milliseconds = System.currentTimeMillis() - time;
        log.info("Finishing with extraction of 2D-doc content of document in {} ms", milliseconds);
        taxDocument.setTime(milliseconds);
        taxDocument.setFileExtractionType(TaxFileExtractionType.TWOD_DOC);
        return taxDocument;
    }

    private TaxDocument getTaxApiCodeContent(String twoDDocContent, String lastName, String firstName, String unaccentFirstName, String unaccentLastName, StringBuilder result) {
        TwoDDoc twoDDoc = utility.parseTwoDDoc(twoDDocContent);
        String fiscalNumber = twoDDoc.getFiscalNumber();
        String referenceNumber = twoDDoc.getReferenceNumber();
        TaxDocument taxDocument = getTaxDocument(lastName, firstName, unaccentFirstName, unaccentLastName, result, fiscalNumber, referenceNumber);
        log.info("Finishing processing tax document with 2D code");
        return taxDocument;
    }

    private StringBuilder getMonFranceConnectCodeContent(TaxDocument taxDocument, StringBuilder currentQrContent, File pdf, String url) {
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
//        log stats mon france connect
        return currentQrContent;
    }

    private TaxDocument processTaxDocumentWithOCR(List<File> files, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        long time = System.currentTimeMillis();
        log.info("Processing tax document without QR code");

        StringBuilder result = new StringBuilder();
        List<File> pdfs = files.stream().filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf")).collect(Collectors.toList());
        if (!pdfs.isEmpty()) {
            for (File pdf : pdfs) {
                result.append(utility.extractInfoFromPDFFirstPage(pdf));
            }
        }
        String fiscalNumber = Utility.extractFiscalNumber(result.toString());
        String referenceNumber = Utility.extractReferenceNumber(result.toString());

        if (StringUtils.isBlank(fiscalNumber) || StringUtils.isBlank(referenceNumber)) {
            String text = files.stream()
                    .map(dfFile -> utility.getTemporaryFile(dfFile))
                    .map(file -> {
                        String extractedText = apiTesseract.extractText(file);
                        try {
                            if (!file.delete()) {
                                log.warn("Unable to delete file");
                            }
                        } catch (Exception e) {
                            log.warn("Unable to delete file", e);
                        }
                        return extractedText;
                    })
                    .reduce("", String::concat);

            if (StringUtils.isBlank(fiscalNumber)) {
                fiscalNumber = Utility.extractFiscalNumber(text);
            }
            if (StringUtils.isBlank(referenceNumber)) {
                referenceNumber = Utility.extractReferenceNumber(text);
            }
        }

        TaxDocument taxDocument = getTaxDocument(lastName, firstName, unaccentFirstName, unaccentLastName, result, fiscalNumber, referenceNumber);
        log.info("Finishing processing tax document without QR code");
        long milliseconds = System.currentTimeMillis() - time;
        taxDocument.setTime(milliseconds);
        taxDocument.setFileExtractionType(TaxFileExtractionType.OCR);
        return taxDocument;
    }

    private TaxDocument getTaxDocument(String lastName, String firstName, String unaccentFirstName, String unaccentLastName, StringBuilder result, String fiscalNumber, String referenceNumber) {
        TaxDocument taxDocument = new TaxDocument();
        boolean test1 = false;
        boolean test2 = false;
        if (StringUtils.isNotBlank(fiscalNumber)) {
            log.info("Call to particulier api");
            ResponseEntity<Taxes> taxesResponseEntity;
            if (newApi) {
                taxesResponseEntity = apiParticulier.particulierApi(fiscalNumber);
            } else {
                if (StringUtils.isBlank(referenceNumber)) {
                    return taxDocument;
                }
                taxesResponseEntity = apiParticulier.particulierApi(fiscalNumber, referenceNumber);
            }

            log.info("Response status {}", taxesResponseEntity.getStatusCodeValue());
            if (taxesResponseEntity.getStatusCode() == HttpStatus.OK) {
                if (newApi) {
                    test1 = test1(Objects.requireNonNull(taxesResponseEntity.getBody()), lastName, firstName, unaccentFirstName, unaccentLastName);
                    test2 = test2(taxesResponseEntity.getBody(), result);
                    taxDocument.setDeclarant1(taxesResponseEntity.getBody().getDeclarant1Name());
                    taxDocument.setDeclarant2(taxesResponseEntity.getBody().getDeclarant2Name());
                    if (taxesResponseEntity.getBody().getRfr() != null) {
                        taxDocument.setAnualSalary(Integer.parseInt(taxesResponseEntity.getBody().getRfr()));
                    } else {
                        taxDocument.setAnualSalary(0);
                    }
                } else {
                    test1 = oldTest1(Objects.requireNonNull(taxesResponseEntity.getBody()), lastName, firstName, unaccentFirstName, unaccentLastName);
                    test2 = oldTest2(taxesResponseEntity.getBody(), result);
                    taxDocument.setDeclarant1(taxesResponseEntity.getBody().getDeclarant1().toString());
                    taxDocument.setDeclarant2(taxesResponseEntity.getBody().getDeclarant2().toString());
                    taxDocument.setAnualSalary(taxesResponseEntity.getBody().getRevenuFiscalReference());
                }
            }
        }

        taxDocument.setTest1(test1);
        taxDocument.setTest2(test2);
        taxDocument.setFiscalNumber(StringUtils.isBlank(fiscalNumber) ? "fail" : fiscalNumber);
        taxDocument.setReferenceNumber(StringUtils.isBlank(referenceNumber) ? "fail" : referenceNumber);
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
                java.io.File tmpFile = utility.getTemporaryFile(pdf);
                String tesseractResult = apiTesseract.extractText(tmpFile);
                try {
                    if (!tmpFile.delete()) {
                        log.warn("Unable to delete file");
                    }
                } catch (Exception e) {
                    log.warn("Unable to delete file", e);
                }

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
    private boolean oldTest1(Taxes taxes, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
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

    public boolean test1(Taxes taxes, String lastName, String firstName, String unaccentFirstName, String unaccentLastName) {
        boolean result1 = (taxes.getNmUsaDec1() != null && StringUtils.containsIgnoreCase(taxes.getNmUsaDec1(), lastName) ||
                taxes.getNmNaiDec1() != null &&
                        StringUtils.containsIgnoreCase(taxes.getNmNaiDec1(), lastName)) && taxes.getPrnmDec1() != null &&
                StringUtils.containsIgnoreCase(taxes.getPrnmDec1(), firstName)
                ||
                (taxes.getNmUsaDec2() != null && (StringUtils.containsIgnoreCase(taxes.getNmUsaDec2(), lastName) ||
                        taxes.getNmNaiDec2() != null && taxes.getPrnmDec2() != null &&
                                StringUtils.containsIgnoreCase(taxes.getNmNaiDec2(), lastName)) &&
                        StringUtils.containsIgnoreCase(taxes.getPrnmDec2(), firstName));

        boolean result2 = (taxes.getNmUsaDec1() != null && (StringUtils.containsIgnoreCase(taxes.getNmUsaDec1(), unaccentLastName) ||
                taxes.getNmNaiDec1() != null && taxes.getPrnmDec1() != null &&
                        StringUtils.containsIgnoreCase(taxes.getNmNaiDec1(), unaccentLastName)) &&
                StringUtils.containsIgnoreCase(taxes.getPrnmDec1(), unaccentFirstName))
                ||
                (taxes.getNmUsaDec2() != null &&
                        ((StringUtils.containsIgnoreCase(taxes.getNmUsaDec2(), unaccentLastName) ||
                                taxes.getNmNaiDec2() != null && taxes.getPrnmDec2() != null &&
                                        StringUtils.containsIgnoreCase(taxes.getNmNaiDec2(), unaccentLastName)) &&
                                StringUtils.containsIgnoreCase(taxes.getPrnmDec2(), unaccentFirstName)));

        return result1 || result2;
    }

    //check if the amount "montant fiscal de référence" is equals to 12*salary with an acceptable error of 10% (value in parameter)
    public boolean test2(Taxes taxes, StringBuilder stringBuilder) {
        if (taxes.getRfr() == null) {
            return false;
        }
        Map<String, Integer> map = Utility.extractNumbersText(stringBuilder.toString());
        int salaryApi = Integer.parseInt(taxes.getRfr());
        return map.containsKey(String.valueOf(salaryApi));
    }

    private boolean oldTest2(Taxes taxes, StringBuilder stringBuilder) {
        Map<String, Integer> map = Utility.extractNumbersText(stringBuilder.toString());
        int salaryApi = taxes.getRevenuFiscalReference();
        return map.containsKey(String.valueOf(salaryApi));
    }
}


