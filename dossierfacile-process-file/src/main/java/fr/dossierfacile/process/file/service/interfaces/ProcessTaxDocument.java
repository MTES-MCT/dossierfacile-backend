package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.type.TaxDocument;

import java.util.Optional;

public interface ProcessTaxDocument {

    Optional<TaxDocument> process(Document document);

}
