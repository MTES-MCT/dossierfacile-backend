package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;

class ApplicationFullMapperTest implements DocumentMappingTest, AuthenticityStatusMappingTest {

    @Override
    public DocumentModel mapDocument(Document document) {
        return new ApplicationFullMapperImpl().toDocumentModel(document);
    }

}