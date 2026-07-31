package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.api.front.application.projection.ApplicationReadView.DocumentReadView;
import fr.dossierfacile.common.application.projection.BaseProjection;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.springframework.stereotype.Component;

@Component
public class DocumentProjection extends BaseProjection<DocumentReadView, DocumentModel> {

    @Override
    public DocumentModel project(DocumentReadView view) {
        Document document = view.document();
        return DocumentModel.builder()
                .id(document.getId())
                .documentCategory(document.getDocumentCategory())
                .documentSubCategory(document.getDocumentSubCategory())
                .subCategory(document.getDocumentSubCategory())
                .documentCategoryStep(document.getDocumentCategoryStep())
                .customText(document.getCustomText())
                .monthlySum(document.getMonthlySum())
                .documentStatus(document.getDocumentStatus())
                .name(view.name())
                .build();
    }
}
