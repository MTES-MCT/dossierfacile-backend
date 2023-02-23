package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.model.AftDetail;
import fr.dossierfacile.process.file.model.ApiDate;
import fr.dossierfacile.process.file.model.Facture;
import fr.dossierfacile.process.file.model.Pac;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ProcessTaxDocument;
import fr.dossierfacile.process.file.util.Utility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ProcessTaxDocumentImplTest {

    private final ProcessTaxDocument processTaxDocument = new ProcessTaxDocumentImpl(null, null, null);

    @Test
    void test1() {
        Taxes taxes = defaultTaxes();
        var firstname = "vincent";
        var lastname = "ac quatorze";
        var res = processTaxDocument.test1(taxes, lastname, firstname, Utility.normalize(firstname), Utility.normalize(lastname));
        assertTrue(res);
    }

    @Test
    void test2() {
        Taxes taxes = defaultTaxes();
        var res = processTaxDocument.test2(taxes, new StringBuilder("string string string 48583 string string string"));
        assertTrue(res);
    }

    private static Taxes defaultTaxes() {
        Taxes taxes = new Taxes();
        taxes.setRfr("48583");
        taxes.setSitFam("M");
        taxes.setNbPart(2);
        var pac = new Pac();
        pac.setNbPac(0);
        taxes.setPac(pac);
        taxes.setNmNaiDec1("AC QUATORZE");
        taxes.setPrnmDec1("VINCENT");
        var birthDate1 = new ApiDate();
        birthDate1.setAnnee("1973");
        birthDate1.setMois("08");
        birthDate1.setJour("02");
        taxes.setDateNaisDec1(birthDate1);
        taxes.setNmNaiDec2("VAORANATITOU");
        taxes.setNmUsaDec2("AC QUATORZE");
        taxes.setPrnmDec2("LEA BRIGITTE");
        var birthDate2 = new ApiDate();
        birthDate2.setAnnee("1973");
        birthDate2.setMois("02");
        birthDate2.setJour("17");
        taxes.setAft("18 RUE DU COMMANDANT MOWAT 94300 VINCENNES");
        AftDetail aftDetail = new AftDetail();
        aftDetail.setCodePostal("94300 VINCENNES");
        aftDetail.setVoie("18 RUE DU COMMANDANT MOWAT");
        taxes.setAftDetail(aftDetail);

        Facture facture1 = new Facture();
        facture1.setNumLigne("109");
        facture1.setCodeLigne("0010");
        facture1.setLibelleLigne("Détail des revenus");
        facture1.setColonne1("Déclar. 1");
        facture1.setColonne2("Déclar. 2");
        facture1.setColonne4("Total");

        Facture facture2 = new Facture();
        facture2.setNumLigne("130");
        facture2.setCodeLigne("0240");
        facture2.setLibelleLigne("Salaires, pensions, rentes nets");
        facture2.setColonne1("22836");
        facture2.setColonne2("34524");
        facture2.setColonne4("57360");

        Facture facture3 = new Facture();
        facture3.setNumLigne("140");
        facture3.setCodeLigne("1000");
        facture3.setLibelleLigne("Revenus fonciers nets");
        facture3.setColonne4("-      9800");

        taxes.setFacture(Arrays.asList(facture1, facture2, facture3));

        return taxes;
    }

}