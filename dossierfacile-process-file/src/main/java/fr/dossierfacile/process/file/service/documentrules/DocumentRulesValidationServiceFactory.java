package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.Document;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DocumentRulesValidationServiceFactory {
    private List<RulesValidationService> rulesValidationServiceList;

    public List<RulesValidationService> getServices(Document document) {
        return rulesValidationServiceList.stream().filter(rvs -> rvs.shouldBeApplied(document)).collect(Collectors.toList());
    }
}