package fr.gouv.bo.dto;

import fr.gouv.bo.utils.UtilsLocatio;

public interface LogActionAgentStatistic {
    int getActions();

    int getAutomatic();

    int getLink();

    int getTotal();

    int getWeek();

    int getYear();

    default String date() {
        return UtilsLocatio.date(getWeek(), getYear());
    }

    default String column3() {
        if (getTotal() != 0) {
            return getAutomatic() + "(" + getAutomatic() * 100 / getTotal() + "%)";
        }
        return "0(0%)";
    }

    default String column5() {
        if (getTotal() != 0) {
            return getLink() + "(" + getLink() * 100 / getTotal() + "%)";
        }
        return "0(0%)";
    }
}
