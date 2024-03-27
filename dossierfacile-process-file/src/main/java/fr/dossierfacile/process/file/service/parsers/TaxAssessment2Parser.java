package fr.dossierfacile.process.file.service.parsers;


import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * A Parsing POC
 */
@Service
@Slf4j
@Order(2)
public class TaxAssessment2Parser extends TaxAssessmentParser implements FileParser<TaxIncomeMainFile> {

    public TaxAssessment2Parser(@Autowired TaxIncomeLeafParser taxIncomeLeafParser) {
        super(taxIncomeLeafParser);
    }

    @Override
    protected String getJsonModelFile() {
        return "/parsers/avisImpotsModele2.json";
    }

}