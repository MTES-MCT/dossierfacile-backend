package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.enums.ParsedFileClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipStandardRulesValidationService extends AbstractPayslipRulesValidationService {

    @Override
    protected ParsedFileClassification getPayslipClassification() {
        return ParsedFileClassification.PAYSLIP;
    }

}