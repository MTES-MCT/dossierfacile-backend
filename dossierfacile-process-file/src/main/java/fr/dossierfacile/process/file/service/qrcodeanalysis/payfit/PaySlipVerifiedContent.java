package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import fr.dossierfacile.common.utils.DateRange;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Getter
public class PaySlipVerifiedContent {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("P[ée]riode du (\\d{2}/\\d{2}/\\d{4}) au (\\d{2}/\\d{2}/\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String companyName;
    private final String companySiret;
    private final String employeeName;
    private final String netSalary;
    private final String grossSalary;
    private final DateRange period;

    public static PaySlipVerifiedContent from(PayfitResponse payFitResponse) {
        var companyInfo = payFitResponse.getContent().getCompanyInfo();
        var employeeInfo = payFitResponse.getContent().getEmployeeInfo();
        return PaySlipVerifiedContent.builder()
                .companyName(extractFrom(companyInfo, "Entreprise"))
                .companySiret(extractFrom(companyInfo, "SIRET"))
                .employeeName(extractFrom(companyInfo, "Employé"))
                .netSalary(extractAmountFrom(employeeInfo, "Net à payer avant impôt"))
                .grossSalary(extractAmountFrom(employeeInfo, "Salaire brut"))
                .period(extractPeriod(payFitResponse.getHeader()))
                .build();
    }

    private static DateRange extractPeriod(String header) {
        Matcher periodMatcher = PERIOD_PATTERN.matcher(header);
        if (periodMatcher.matches()) {
            return DateRange.of(LocalDate.parse(periodMatcher.group(1), DATE_TIME_FORMATTER),
                    LocalDate.parse(periodMatcher.group(2), DATE_TIME_FORMATTER));
        }
        return null;
    }

    private static String extractFrom(List<PayfitResponse.Info> list, String label) {
        return list.stream()
                .filter(info -> label.equalsIgnoreCase(info.getLabel()))
                .findFirst()
                .map(PayfitResponse.Info::getValue)
                .orElse("");
    }

    private static String extractAmountFrom(List<PayfitResponse.Info> list, String label) {
        PayfitAmount amount = new PayfitAmount(extractFrom(list, label));
        return amount.format();
    }
}
