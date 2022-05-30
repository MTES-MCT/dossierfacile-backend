package fr.dossierfacile.common.type;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxDocument implements Serializable {
    private static final long serialVersionUID = -7399177020185744483L;
    private String declarant1;
    private String declarant2;
    private int anualSalary;
    private boolean test1 = false;
    private boolean test2 = false;
    private String fiscalNumber;
    private String referenceNumber;
    private long time = 0;
    private String qrContent;
    private Boolean taxContentValid;
}
