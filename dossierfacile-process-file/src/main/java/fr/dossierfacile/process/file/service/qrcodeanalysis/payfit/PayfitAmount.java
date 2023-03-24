package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import lombok.AllArgsConstructor;

import java.text.NumberFormat;
import java.util.Locale;

@AllArgsConstructor
class PayfitAmount {

    private final String rawValue;

    String format() {
        try {
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.FRANCE);
            Double amount = Double.valueOf(rawValue);
            return nf.format(amount)
                    .replaceAll("[  ]", " ");
        } catch (NumberFormatException e) {
            return rawValue;
        }
    }

}
