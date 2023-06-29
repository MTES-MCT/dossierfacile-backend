package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;

class TenantCommonMapperTest implements DocumentMappingTest {

    @Override
    public DocumentModel mapDocument(Document document) {
        return new TenantCommonMapperImpl().toDocumentModel(document);
    }

}