package fr.gouv.owner.dto;


import fr.gouv.owner.utils.UtilsLocatio;

public interface CountDTO {

    long getCount();

    int getWeek();

    int getYear();

    default String date() {
        return UtilsLocatio.date(getWeek(), getYear());
    }
}
