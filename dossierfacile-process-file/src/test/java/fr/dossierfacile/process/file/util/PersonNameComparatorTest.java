package fr.dossierfacile.process.file.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PersonNameComparatorTest {
    @Test
    void compare_name_should_be_ok() {
        String givenFirstName = "Jean Phillipe";
        String givenLastName = "DE LA MARCHE";
        String fullName = "DE LA MARCHE PICQUET Edouard Jean Phillipe";
        Assertions.assertEquals(true, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_ko_particule() {
        String givenFirstName = "Jean Phillipe";
        String givenLastName = "DE LA ROSE";
        String fullName = "DE LA MARCHE PICQUET Edouard Jean Phillipe";
        Assertions.assertEquals(false, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_ko() {
        String givenFirstName = "Jean Phillipe";
        String givenLastName = "DE LA MARCHE";
        String fullName = "PICQUET Edouard Jean Phillipe";
        Assertions.assertEquals(false, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_ok2() {
        String givenFirstName = "Jean Phillipe";
        String givenLastName = "MARCHE";
        String fullName = "ROSE-MARCHE Jean";
        Assertions.assertEquals(true, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_ok_basc() {
        String givenFirstName = "Jean";
        String givenLastName = "MARCHE";
        String fullName = "MARCHE Jean";
        Assertions.assertEquals(true, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_ok_apostrophe() {
        String givenFirstName = "Jean";
        String givenLastName = "M BOL";
        String fullName = "M'Bol Jean";
        Assertions.assertEquals(true, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }

    @Test
    void compare_name_should_be_nok_reverse() {
        String givenFirstName = "Marche";
        String givenLastName = "Jean";
        String fullName = "MARCHE Jean";
        Assertions.assertEquals(false, PersonNameComparator.bearlyEqualsTo(fullName, givenLastName, givenFirstName));
    }
}