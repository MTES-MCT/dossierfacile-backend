package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class Documents {

    private final List<Document> documents;

    public static Documents of(Tenant tenant) {
        return new Documents(tenant.getDocuments());
    }

    public static Documents of(Guarantor guarantor) {
        return new Documents(guarantor.getDocuments());
    }

    public static Documents ofTenantAndGuarantors(Tenant tenant) {
        var documents = Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .stream()
                .map(Guarantor::getDocuments)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        documents.addAll(tenant.getDocuments());
        return new Documents(documents);
    }

    public List<Document> byCategory(DocumentCategory documentCategory) {
        return documents.stream()
                .filter(doc -> doc.getDocumentCategory() == documentCategory)
                .filter(doc -> !doc.getNoDocument())
                .collect(Collectors.toList());
    }

    public List<Document> byCategories(List<DocumentCategory> documentCategories) {
        return documents.stream()
                .filter(doc -> documentCategories.contains(doc.getDocumentCategory()))
                .filter(doc -> !doc.getNoDocument())
                .collect(Collectors.toList());
    }

}
