package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TenantSituation {
    CDI("tenant.form.situation.cdi", Constants.OPTION3, Constants.OPTION5),
    CDI_TRIAL("tenant.form.situation.cdi_trial", Constants.OPTION3, Constants.OPTION5),
    CDD("tenant.form.situation.cdd", Constants.OPTION3, Constants.OPTION5),
    INTERNSHIP("tenant.form.situation.internship", "bo.tenant.custom.email.option3.internship", Constants.OPTION5),
    ALTERNATION("tenant.form.situation.alternation", "bo.tenant.custom.email.option3.alternation", Constants.OPTION5),
    STUDENT("tenant.form.situation.student", "bo.tenant.custom.email.option3.student", "bo.tenant.custom.email.option5.student"),
    PUBLIC("tenant.form.situation.public", "bo.tenant.custom.email.option3.public", Constants.OPTION5),
    CTT("tenant.form.situation.ctt", Constants.OPTION3, Constants.OPTION5),
    RETIRED("tenant.form.situation.retired", "bo.tenant.custom.email.option3.retired", "bo.tenant.custom.email.option5.retired"),
    UNEMPLOYED("tenant.form.situation.unemployed", "bo.tenant.custom.email.option3.unemployed", "bo.tenant.custom.email.option5.unemployed"),
    INDEPENDENT("tenant.form.situation.independent", "bo.tenant.custom.email.option3.independent", "bo.tenant.custom.email.option5.independent"),
    OTHER("tenant.form.situation.other", "bo.tenant.custom.email.option3.other", "bo.tenant.custom.email.option5.other"),
    UNDEFINED;
    String label;
    String messageFile3;
    String messageFile5;

    private static class Constants {
        private static final String OPTION3 = "bo.tenant.custom.email.option3";
        private static final String OPTION5 = "bo.tenant.custom.email.option5";
    }
}
