package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.type.TaxDocument;

public interface ProcessTaxDocument {
    TaxDocument process(Document d, Tenant tenant);
}
