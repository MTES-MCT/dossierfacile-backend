package fr.gouv.bo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Calendar;
import java.util.List;

@Getter
@Setter
public class WeekRangeDTO {

    List<List<String>> weekdates;

    Calendar calendar;

    public WeekRangeDTO(List<List<String>> weekdates, Calendar calendar) {
        this.weekdates = weekdates;
        this.calendar = calendar;
    }
}
