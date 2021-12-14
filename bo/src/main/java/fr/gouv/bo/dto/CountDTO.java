package fr.gouv.bo.dto;


import fr.gouv.bo.utils.UtilsLocatio;

public interface CountDTO {
    long getCount();

    int getWeek();

    int getYear();

    default String date() {
        return UtilsLocatio.date(getWeek(), getYear());
    }
}
