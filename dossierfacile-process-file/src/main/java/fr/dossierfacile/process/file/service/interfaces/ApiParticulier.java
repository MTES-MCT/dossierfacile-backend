package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.process.file.model.Taxes;

public interface ApiParticulier {
    Taxes particulierApi(String fiscalNumber, String taxReference);
}
