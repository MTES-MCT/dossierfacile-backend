package fr.dossierfacile.process.file.util;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public class PersonNameComparator {
    private static String normalizeName(String name) {
        if (name == null)
            return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized.replace('-', ' ').replace('.', ' ')
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toUpperCase().trim();
    }
    public static boolean equalsWithNormalization(String fullName, String fullNameToCompare) {
        return normalizeName(fullName).equals(normalizeName(fullNameToCompare));
    }

    public static boolean bearlyEqualsTo(String name, String givenName) {

        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(givenName))
            return false;
        List<String> names = Arrays.stream(normalizeName(name).split(" "))
                .filter((n) -> !n.matches("(DE)|(LE)|(LA)|(EL)")).toList();
        List<String> givenNames = Arrays.stream(normalizeName(givenName).split(" "))
                .filter((n) -> !n.matches("(DE)|(LE)|(LA)|(EL)")).toList();

        // if there is one matching on firstname and one matching on lastname the user should be the same
        return names.stream().anyMatch(givenNames::contains);
    }

    /**
     * fullname start by LastName
     **/
    public static boolean bearlyEqualsTo(String fullName, String lastName, String firstName) {
        if (StringUtils.isEmpty(fullName) || StringUtils.isEmpty(firstName) || StringUtils.isEmpty(lastName))
            return false;
        String full = normalizeName(fullName);
        String[] allFull = full.split(" ");
        if (allFull.length < 2)
            return false;
        // Lastname can be all the string excluding the LAST one;
        List<String> lastnames = Arrays.stream(allFull).limit(allFull.length - 1).toList();
        // firstname can be all the string excluding the FIRST one;
        List<String> firstnames = Arrays.stream(allFull).skip(1).toList();

        //
        String givenLastName = normalizeName(lastName);
        List givenLastNames = Arrays.stream(givenLastName.split(" ")).filter((n) -> !n.matches("(DE)|(LE)|(LA)|(EL)")).toList();
        String givenFirstName = normalizeName(firstName);
        List givenFirstNames = Arrays.stream(givenFirstName.split(" ")).toList();

        // if there is one matching on firstname and one matching on lastname the user should be the same
        return lastnames.stream().anyMatch(givenLastNames::contains)
                && firstnames.stream().anyMatch(givenFirstNames::contains);
    }

}
