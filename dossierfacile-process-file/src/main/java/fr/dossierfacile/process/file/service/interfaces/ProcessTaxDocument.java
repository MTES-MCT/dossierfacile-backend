package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.type.TaxDocument;
import fr.dossierfacile.process.file.model.Taxes;

public interface ProcessTaxDocument {
    TaxDocument process(Document document, Guarantor guarantor);

    TaxDocument process(Document d, Tenant tenant);
    boolean test1(Taxes taxes, String lastName, String firstName, String unaccentFirstName, String unaccentLastName);

    boolean test2(Taxes taxes, StringBuilder stringBuilder);
}
