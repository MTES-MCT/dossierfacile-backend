package fr.dossierfacile.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum DocumentSubCategory {

    // Identification
    FRENCH_IDENTITY_CARD,
    FRENCH_PASSPORT,
    FRENCH_RESIDENCE_PERMIT,
    DRIVERS_LICENSE,
    OTHER_IDENTIFICATION,
    CERTIFICATE_VISA,

    // Residency
    TENANT,
    OWNER,
    GUEST_PARENTS,
    GUEST,
    GUEST_ORGANISM,
    SHORT_TERM_RENTAL,
    OTHER_RESIDENCY,

    // Professional
    CDI,
    CDI_TRIAL,
    CDD,
    ALTERNATION,
    INTERNSHIP,
    STUDENT,
    PUBLIC,
    CTT,
    RETIRED,
    UNEMPLOYED,
    INDEPENDENT,
    INTERMITTENT,
    STAY_AT_HOME_PARENT,
    NO_ACTIVITY,
    ARTIST,
    OTHER,

    // Financial
    SALARY,
    SCHOLARSHIP,
    SOCIAL_SERVICE,
    RENT,
    PENSION,
    NO_INCOME,

    // Tax
    MY_NAME,
    MY_PARENTS,
    LESS_THAN_YEAR,
    OTHER_TAX,

    LEGAL_PERSON,
    UNDEFINED,
    OTHER_PROFESSION_GUARANTOR;

    public static List<DocumentSubCategory> alphabeticallySortedValues() {
        List<DocumentSubCategory> values = Arrays.asList(values());
        values.sort(Comparator.comparing(DocumentSubCategory::name));
        return values;
    }

    public DocumentSubCategory getOnlyOldCategories() {
        return switch (this) {
            case INTERMITTENT, STAY_AT_HOME_PARENT, NO_ACTIVITY, ARTIST -> OTHER;
            case DRIVERS_LICENSE -> OTHER_IDENTIFICATION;
            case SHORT_TERM_RENTAL -> TENANT;
            case GUEST_ORGANISM, OTHER_RESIDENCY -> GUEST;
            default -> this;
        };
    }

}
