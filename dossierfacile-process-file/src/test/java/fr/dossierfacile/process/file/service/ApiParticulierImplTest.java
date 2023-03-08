package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.IntegrationTest;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@IntegrationTest
class ApiParticulierImplTest {

    @Autowired
    private ApiParticulier apiParticulier;

    @Test
    void particulierApi() {
        ResponseEntity<Taxes> response = apiParticulier.particulierApi("3999999899410");
        assertNotNull(response.getBody());
        assertEquals("AC QUATORZE", response.getBody().getNmNaiDec1());
        assertEquals("48583", response.getBody().getRfr());
    }

}