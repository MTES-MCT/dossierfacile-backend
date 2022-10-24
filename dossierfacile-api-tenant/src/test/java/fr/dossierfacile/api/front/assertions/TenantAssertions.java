package fr.dossierfacile.api.front.assertions;

import org.assertj.core.api.Assertions;
import sibModel.SendSmtpEmail;

public class TenantAssertions extends Assertions {

    public static SendSmtpEmailAssert assertThat(SendSmtpEmail email) {
        return new SendSmtpEmailAssert(email);
    }

}
