package fr.gouv.bo.dto;


import fr.gouv.bo.utils.UtilsLocatio;

public interface SuperFacileDTO {
    int getComplete();

    int getEmail();

    int getInfo();

    int getName();

    int getTotal();

    int getWeek();

    int getYear();

    default String date() {
        return UtilsLocatio.date(getWeek(), getYear());
    }

    default String b() {
        if (getTotal() != 0) {
            return getInfo() + "(" + getInfo() * 100 / getTotal() + "%)";
        }
        return "0(0%)";
    }

    default String c() {
        if (getInfo() != 0) {
            return getName() + "(" + getName() * 100 / getInfo() + "%)";
        }
        return "0(0%)";
    }

    default String d() {
        if (getName() != 0) {
            return getEmail() + "(" + getEmail() * 100 / getName() + "%)";
        }
        return "0(0%)";
    }

    default String e() {
        if (getTotal() != 0) {
            return getComplete() + "(" + getComplete() * 100 / getTotal() + "%)";
        }
        return "0(0%)";
    }
}
