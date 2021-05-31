package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.mapper.ApplicationFullMapper;
import fr.dossierfacile.api.front.mapper.ApplicationLightMapper;
import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.api.front.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.front.repository.LinkLogRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingServiceImpl implements ApartmentSharingService {
    private final Locale locale = LocaleContextHolder.getLocale();

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final ApplicationLightMapper applicationLightMapper;
    private final OvhService ovhService;
    private final MessageSource messageSource;
    private final LinkLogRepository linkLogRepository;

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public void createApartmentSharing(Tenant tenant) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId()).orElse(new ApartmentSharing(tenant));
        apartmentSharingRepository.save(apartmentSharing);
        tenant.setApartmentSharing(apartmentSharing);
        tenantRepository.save(tenant);
    }

    @Override
    public ApplicationModel full(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));
        saveLinkLog(apartmentSharing, token, LinkType.FULL_APPLICATION);
        return applicationFullMapper.toApplicationModel(apartmentSharing);
    }

    @Override
    public ApplicationModel light(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTokenPublic(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));
        saveLinkLog(apartmentSharing, token, LinkType.LIGHT_APPLICATION);
        return applicationLightMapper.toApplicationModel(apartmentSharing);
    }

    @Override
    public ByteArrayOutputStream fullPdf(String token) throws UnexpectedException {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));
        PDFMergerUtility ut = new PDFMergerUtility();
        List<Tenant> tenantsValidated = apartmentSharing.getTenants().stream().filter(t -> t.getStatus() == TenantFileStatus.VALIDATED).sorted(Comparator.comparing(Tenant::getTenantType)).collect(Collectors.toList());
        int numberOfTenantsValidated = tenantsValidated.size();
        if (numberOfTenantsValidated == 0) {
            throw new UnexpectedException("Full pdf generation in an apartment that doesn't have all tenants as validated.");
        }
        saveLinkLog(apartmentSharing, token, LinkType.DOCUMENT);
        List<Integer> pages = new ArrayList<>();
        pages.add(numberOfTenantsValidated + 1);
        ut.addSource(new ByteArrayInputStream(createFirstsPages(numberOfTenantsValidated).toByteArray()));
        for (Tenant tenant1 : tenantsValidated) {
            List<Document> documentsOrderedByCategory = tenant1.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).collect(Collectors.toList());
            DocumentCategory previousCategory = documentsOrderedByCategory.get(0).getDocumentCategory();
            boolean firstDocumentSubject = true;
            for (Document document : documentsOrderedByCategory) {
                DocumentCategory currentCategory = document.getDocumentCategory();
                log.info("Downloading document [" + currentCategory.name() + "]");
                String urlPdfDocument = document.getName();
                if (urlPdfDocument != null && !urlPdfDocument.isBlank()) {
                    SwiftObject swiftObject = ovhService.get(urlPdfDocument);
                    addDocument(ut, swiftObject, pages, firstDocumentSubject || previousCategory != currentCategory);
                    firstDocumentSubject = false;
                    previousCategory = currentCategory;
                }
            }

            List<Guarantor> guarantorsOrderedByType = tenant1.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor)).collect(Collectors.toList());
            for (Guarantor guarantor1 : guarantorsOrderedByType) {
                documentsOrderedByCategory = guarantor1.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).collect(Collectors.toList());
                previousCategory = documentsOrderedByCategory.get(0).getDocumentCategory();
                firstDocumentSubject = true;
                for (Document document : documentsOrderedByCategory) {
                    DocumentCategory currentCategory = document.getDocumentCategory();

                    String urlPdfDocument = document.getName();
                    if (urlPdfDocument != null && !urlPdfDocument.isBlank()) {
                        SwiftObject swiftObject = ovhService.get(urlPdfDocument);
                        addDocument(ut, swiftObject, pages, firstDocumentSubject || previousCategory != currentCategory);
                        firstDocumentSubject = false;
                        previousCategory = currentCategory;
                    }
                }
            }
        }
        ByteArrayOutputStream merge = new ByteArrayOutputStream();
        ut.setDestinationStream(merge);
        try {
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error("Problem merge document for pdf full");
            log.error(e.getMessage(), e.getCause());
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(merge.toByteArray()))) {
            addPaginate(doc);
            Resource resource = new ClassPathResource("static/fonts/ArialNova-Light.ttf");
            PDType0Font font = PDType0Font.load(doc, resource.getInputStream());
            Tenant tenant = tenantsValidated.stream().filter(t -> t.getTenantType() == TenantType.CREATE).findFirst().orElseThrow(() -> new TenantNotFoundException(TenantType.CREATE));
            String title = getTitle(tenantsValidated, tenant);
            int indexPage = 0;
            int totalPages = doc.getNumberOfPages();
            for (int i = 0; i < tenantsValidated.size() && totalPages > tenantsValidated.size(); i++) {
                Tenant t = tenantsValidated.get(i);
                PDPageContentStream contentStream = new PDPageContentStream(doc, doc.getPage(i), PDPageContentStream.AppendMode.APPEND, true);
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.setNonStrokingColor(74 / 255.0F, 144 / 255.0F, 226 / 255.0F);
                contentStream.newLineAtOffset(50, 700);
                contentStream.setLeading(14.5f);
                contentStream.showText(title);
                contentStream.newLine();
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText(title(t));
                contentStream.newLine();
                contentStream.newLine();
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText(t.getFullName());
                contentStream.newLine();

                //We obtain here the list with the categories, NOT REPEATED (distinctByKey), of documents that the tenant has. Ordered ascending by the ID of DocumentCategory.
                List<DocumentCategory> listOfDocumentCategoryContainedForTenant = t.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).filter(distinctByKey(Document::getDocumentCategory)).map(Document::getDocumentCategory).collect(Collectors.toList());
                for(DocumentCategory documentCategory: listOfDocumentCategoryContainedForTenant) {
                    contentStream.showText("p." + pages.get(indexPage++) + " - " + messageSource.getMessage(t.getTenantSituation().getText(documentCategory.ordinal() + 1), null, locale));
                    contentStream.newLine();
                }
                List<Guarantor> guarantors = t.getGuarantors().stream().sorted(Comparator.comparing(Guarantor::getTypeGuarantor)).collect(Collectors.toList());
                for (Guarantor guarantor : guarantors) {
                    contentStream.newLine();
                    contentStream.newLine();

                    TypeGuarantor typeGuarantor = guarantor.getTypeGuarantor();

                    String guarantorText = "";
                    switch (typeGuarantor) {
                        case NATURAL_PERSON -> guarantorText = "Personne physique - " + guarantor.getCompleteName();
                        case LEGAL_PERSON -> guarantorText = "Personne morale";
                        case ORGANISM -> guarantorText = "Organisme";
                    }

                    contentStream.showText("GARANT - " + guarantorText);
                    contentStream.newLine();

                    //We obtain here the list with the categories, NOT REPEATED (distinctByKey), of documents that the guarantor has. Ordered ascending by the ID of DocumentCategory.
                    List<DocumentCategory> listOfDocumentCategoryContainedForGuarantor = guarantor.getDocuments().stream().sorted(Comparator.comparing(Document::getDocumentCategory)).filter(distinctByKey(Document::getDocumentCategory)).map(Document::getDocumentCategory).collect(Collectors.toList());
                    for(DocumentCategory documentCategory: listOfDocumentCategoryContainedForGuarantor) {
                        switch (typeGuarantor) {
                            case NATURAL_PERSON -> contentStream.showText("p." + pages.get(indexPage++) + " - " + messageSource.getMessage(t.getTenantSituation().getText(documentCategory.ordinal() + 1), null, locale));
                            case LEGAL_PERSON -> {
                                switch (documentCategory) {
                                    case IDENTIFICATION -> contentStream.showText("p." + pages.get(indexPage++) + " - Justificatif d'identité du représentant de l'organisme");
                                    case IDENTIFICATION_LEGAL_PERSON -> contentStream.showText("p." + pages.get(indexPage++) + " - Attestation de garantie de l'organisme");
                                }
                            }
                            case ORGANISM -> contentStream.showText("p." + pages.get(indexPage++) + " - Visa de l'organisme");
                        }
                        contentStream.newLine();
                    }
                }
                contentStream.endText();
                contentStream.close();
            }
            doc.save(result);
            log.info("Generation completed");
        } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Problem creating full pdf");
            log.error(e.getMessage(), e.getCause());
        }
        return result;
    }

    private void addDocument(PDFMergerUtility ut, SwiftObject swiftObject, List<Integer> pages, boolean newCategoryDocument) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (swiftObject != null) {
            try {
                IOUtils.copy(swiftObject.download().getInputStream(), baos);
            } catch (IOException e) {
                log.error("Problem copy inputStream to outputStream in addDocument method");
                log.error(e.getMessage(), e.getCause());
            }
            ut.addSource(new ByteArrayInputStream(baos.toByteArray()));
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(baos.toByteArray()))) {
                if (newCategoryDocument) {
                    pages.add(pages.get(pages.size() - 1) + document.getNumberOfPages());
                } else {
                    pages.set(pages.size() - 1, pages.get(pages.size() - 1) + document.getNumberOfPages());
                }
            } catch (IOException e) {
                log.error("Problem with document in addDocument method");
                log.error(e.getMessage(), e.getCause());
            }
        }
    }

    private ByteArrayOutputStream createFirstsPages(int numberOfTenantsValidated) {
        PDFMergerUtility ut = new PDFMergerUtility();
        Resource resource = new ClassPathResource("static/pdf/template_dossier_pdf_first_pages.pdf");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for (int i = 0; i < numberOfTenantsValidated; i++) {
                ut.addSource(resource.getInputStream());
            }
            ut.setDestinationStream(outputStream);
            ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
        }
        return outputStream;
    }

    private void addPaginate(PDDocument doc) {
        PDFont font = PDType1Font.HELVETICA_BOLD;
        float fontSize = 12;
        int numberPage = 1;
        int totalPages = doc.getNumberOfPages();
        for (PDPage page : doc.getPages()) {
            PDRectangle pageSize = page.getMediaBox();
            float pageWidth = pageSize.getWidth();
            float centerX = pageWidth - 50;
            float centerY = 20;

            // append the content to the existing stream
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.setTextMatrix(Matrix.getTranslateInstance(centerX, centerY));
                contentStream.showText(numberPage++ + "/" + totalPages);
                contentStream.endText();
            } catch (IOException e) {
                log.error("Error adding paginate to create full pdf");
                log.error(e.getMessage(), e.getCause());
            }
        }
    }

    private String getTitle(List<Tenant> tenantList, Tenant tenant) {
        int size = tenantList.size();
        StringBuilder title = new StringBuilder();
        if (size == 1 && tenant.getGuarantors() != null) {
            title.append(tenant.getFullName());
            title.append(" est seul avec un garant");
        }
        if (size == 1 && tenant.getGuarantors() == null) {
            title.append(tenant.getFullName());
            title.append(" est seul sans garant");
        }
        if (size == 2 && tenant.getApartmentSharing().getTotalGuarantor() > 0) {
            title.append(tenantList.get(0).getFullName());
            title.append(" et ");
            title.append(tenantList.get(1).getFullName());
            title.append(" sont en couple avec ");
            title.append(tenant.getApartmentSharing().getTotalGuarantor());
            title.append(" garants");
        }
        if (size == 2 && tenant.getApartmentSharing().getTotalGuarantor() == 0) {
            title.append(tenantList.get(0).getFullName());
            title.append(" et ");
            title.append(tenantList.get(1).getFullName());
            title.append(" sont en couple sans garants");
        }
        if (size > 2) {
            for (int i = 2; i < size; i++) {
                title.append(tenantList.get(i).getFullName());
                title.append(", ");
            }
            if (tenant.getApartmentSharing().getTotalGuarantor() > 0) {
                title.append(tenantList.get(0).getFullName());
                title.append(" et ");
                title.append(tenantList.get(1).getFullName());
                title.append(" sont en colocation avec ");
                title.append(tenant.getApartmentSharing().getTotalGuarantor());
                title.append(" garants");
            }
            if (tenant.getApartmentSharing().getTotalGuarantor() == 0) {
                title.append(tenantList.get(0).getFullName());
                title.append(" et ");
                title.append(tenantList.get(1).getFullName());
                title.append(" sont en colocation sans garants");
            }
        }
        return title.toString();
    }

    private String title(Tenant tenant) {
        return tenant.getFullName() + " est en " + messageSource.getMessage(tenant.getTenantSituation().getLabel(), null, locale) + " avec un revenu net mensuel de " + tenant.getTotalSalary() + " €. " + ((tenant.getClarification() != null && !tenant.getClarification().isBlank()) ? tenant.getClarification() : "");
    }

    private void saveLinkLog(ApartmentSharing apartmentSharing, String token, LinkType linkType) {
        linkLogRepository.save(new LinkLog(
                apartmentSharing,
                token,
                linkType
        ));
    }
}
