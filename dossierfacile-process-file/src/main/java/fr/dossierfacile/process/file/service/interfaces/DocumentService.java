package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.common.type.TaxDocument;

public interface DocumentService {
    void updateTaxProcessResult(TaxDocument taxProcessResult, Long documentId);
}
