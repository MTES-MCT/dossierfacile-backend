package fr.dossierfacile.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    private LocalDate start;
    private LocalDate end;

    public static DateRange of(LocalDate start, LocalDate end) {
        return new DateRange(start, end);
    }
}