package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiParticulierImplTest {

    @Autowired
    private ApiParticulier apiParticulier;

    @Test
    @Tag("IntegrationTest")
    void particulierApi() {
        ResponseEntity<Taxes> response = apiParticulier.particulierApi("3999999899410");
        assertNotNull(response.getBody());
        assertEquals("AC QUATORZE", response.getBody().getNmNaiDec1());
        assertEquals("48583", response.getBody().getRfr());
    }

}