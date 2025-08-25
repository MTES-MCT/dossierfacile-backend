package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.Document;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DocumentRulesValidationServiceFactory {
    private List<AbstractRulesValidationService> rulesValidationServiceList;

    public List<AbstractRulesValidationService> getServices(Document document) {
        return rulesValidationServiceList.stream().filter(rvs -> rvs.shouldBeApplied(document)).toList();
    }
}