package fr.gouv.owner.dto;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyApartmentSharingDTO {

    private List<PropertyApartmentSharing> propertyApartmentSharingList;

    public boolean getPropertyApartmentSharing(List<PropertyApartmentSharing> propertyApartmentSharingList, Tenant tenant) {
        PropertyApartmentSharing result = propertyApartmentSharingList.stream().filter(propertyApartmentSharing ->
                propertyApartmentSharing.getApartmentSharing().getId().equals(tenant.getApartmentSharing().getId())).findAny().orElse(null);
        assert result != null;
        if (result.isAccessFull()) {
            return true;
        }
        return false;
    }
    public Document getDocumentForTenant(Tenant tenant, String category) {
        Document document = tenant.getDocuments().stream().filter(document1 ->
                document1.getDocumentCategory().toString().equals(category)).findAny().orElse(null);
        if (document == null) {
            Document doc = new Document();
            doc.setName("NOT_FOUND");
            return doc;
        }
        return document;
    }

    public List<Document> getFinancialDocumentForTenant(Tenant tenant){
        List<Document> documentListFinancial = new ArrayList<>();
        tenant.getDocuments().forEach(document -> {
            if(document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)){
                documentListFinancial.add(document);
            }
        });
        return documentListFinancial;
    }

    public List<Document> getFinancialDocumentForGuarantor(Guarantor guarantor){
        List<Document> documentListFinancial = new ArrayList<>();
        guarantor.getDocuments().forEach(document -> {
            if(document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)){
                documentListFinancial.add(document);
            }
        });
        return documentListFinancial;
    }

    public Boolean getOneFinancialDocumentGuarantor(Guarantor guarantor){
        List<Document> documentListFinancial = new ArrayList<>();
        guarantor.getDocuments().forEach(document -> {
            if(document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)){
                documentListFinancial.add(document);
            }
        });
        if (documentListFinancial.isEmpty()){
            return true;
        }
        return false;
    }

    public Boolean getOneFinancialDocument(Tenant tenant){
        List<Document> documentListFinancial = new ArrayList<>();
        tenant.getDocuments().forEach(document -> {
            if(document.getDocumentCategory().equals(DocumentCategory.FINANCIAL)){
                documentListFinancial.add(document);
            }
        });
        if (documentListFinancial.isEmpty()){
            return true;
        }
        return false;
    }

    public Document getDocumentForTenantGuarantor(Tenant tenant, String category) {
        Document defaultDoc = new Document();
        defaultDoc.setName("NOT_FOUND");
        for (Guarantor guarant : tenant.getGuarantors()) {
            for (Document document : guarant.getDocuments()) {
                if (document.getDocumentCategory().toString().equals(category)) {
                    defaultDoc.setName(document.getName());
                    defaultDoc.setDocumentStatus(document.getDocumentStatus());
                }
            }
        }
        return defaultDoc;
    }

    public Document getDocumentForGuarantor(Guarantor guarantor, String category) {
        Document defaultDoc = new Document();
        defaultDoc.setName("NOT_FOUND");
        for (Document document : guarantor.getDocuments()) {
            if (document.getDocumentCategory().toString().equals(category)) {
                defaultDoc.setName(document.getName());
                defaultDoc.setDocumentStatus(document.getDocumentStatus());
            }
        }
        return defaultDoc;
    }

    public Guarantor getTypeGuarantorForTenant(Tenant tenant) {
        Guarantor guarantor = tenant.getGuarantors().stream().findFirst().orElse(null);
        assert guarantor != null;
        return guarantor;
    }

    public List<Tenant> orderTenant(List<Tenant> list) {
        return list.stream().sorted(Comparator.comparing(Tenant::getId)).collect(Collectors.toList());

    }

}


