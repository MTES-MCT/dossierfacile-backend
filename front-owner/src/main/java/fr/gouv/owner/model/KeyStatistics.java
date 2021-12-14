package fr.gouv.owner.model;

import fr.gouv.owner.utils.UtilsLocatio;

public class KeyStatistics implements Comparable<KeyStatistics> {
    private int week;
    private int year;

    public KeyStatistics(int week, int year) {
        this.week = week;
        this.year = year;
    }

    public KeyStatistics() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        KeyStatistics keyStatistics = (KeyStatistics) obj;
        return keyStatistics.getWeek() == this.week && keyStatistics.getYear() == this.year;
    }

    @Override
    public int hashCode() {
        return week * year;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public int compareTo(KeyStatistics o) {
        int result = Integer.compare(year, o.getYear());
        if (result == 0) {
            result = Integer.compare(week, o.getWeek());
        }
        return result;
    }

    public String transform() {
        return UtilsLocatio.date(week, year);
    }
}
