package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.process.file.model.Taxes;
import org.springframework.http.ResponseEntity;

public interface ApiParticulier {
    ResponseEntity<Taxes> particulierApi(String fiscalNumber, String taxReference);
}
