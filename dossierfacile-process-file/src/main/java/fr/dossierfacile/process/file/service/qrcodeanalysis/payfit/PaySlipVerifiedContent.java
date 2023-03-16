package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitResponse;
import lombok.Builder;

import java.util.List;
import java.util.stream.Stream;

@Builder
public class PaySlipVerifiedContent {

    private final String companyName;
    private final String companySiret;
    private final String employeeName;
    private final PayfitAmount netSalary;
    private final PayfitAmount grossSalary;

    public static PaySlipVerifiedContent from(PayfitResponse payFitResponse) {
        var companyInfo = payFitResponse.getContent().getCompanyInfo();
        var employeeInfo = payFitResponse.getContent().getEmployeeInfo();
        return PaySlipVerifiedContent.builder()
                .companyName(extractFrom(companyInfo, "Entreprise"))
                .companySiret(extractFrom(companyInfo, "SIRET"))
                .employeeName(extractFrom(companyInfo, "Employé"))
                .netSalary(extractAmountFrom(employeeInfo, "Net à payer avant impôt"))
                .grossSalary(extractAmountFrom(employeeInfo, "Salaire brut"))
                .build();
    }

    public boolean isMatchingWith(String fileContent) {
        return Stream.of(companyName, companySiret, employeeName, netSalary.format(), grossSalary.format())
                .allMatch(fileContent::contains);
    }

    private static String extractFrom(List<PayfitResponse.Info> list, String label) {
        return list.stream()
                .filter(info -> label.equals(info.getLabel()))
                .findFirst()
                .map(PayfitResponse.Info::getValue)
                .orElse("");
    }

    private static PayfitAmount extractAmountFrom(List<PayfitResponse.Info> list, String label) {
        return new PayfitAmount(extractFrom(list, label));
    }

}
