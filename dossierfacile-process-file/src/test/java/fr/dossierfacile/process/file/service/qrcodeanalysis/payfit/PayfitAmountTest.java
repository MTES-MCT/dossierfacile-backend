package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PayfitAmountTest {

    @ParameterizedTest
    @CsvSource(value = {
            "1234.56; 1 234,56 €",
            "10.56; 10,56 €",
            "1; 1,00 €",
    }, delimiter = ';')
    void format_amount(String input, String expected) {
        PayfitAmount amount = new PayfitAmount(input);
        assertThat(amount.format()).isEqualTo(expected);
    }

}