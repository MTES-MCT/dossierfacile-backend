package fr.gouv.owner.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class WeeklyStatisticDTO {

    private int week;
    private int year;
    private long value;

    public WeeklyStatisticDTO(int week, int year, long value) {
        super();
        this.week = week;
        this.year = year;
        this.value = value;
    }
}
