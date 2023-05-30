package fr.dossierfacile.api.dossierfacileapiowner.webhook;

import fr.dossierfacile.api.dossierfacileapiowner.mail.MailService;
import fr.dossierfacile.api.dossierfacileapiowner.property.PropertyRepository;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {
    private final PropertyRepository propertyRepository;
    private final MailService mailService;
    @Override
    public void handleWebhook(ApplicationModel applicationModel) {
        if (applicationModel.getStatus() == TenantFileStatus.VALIDATED) {
            List<Long> tenantIds = applicationModel.getTenants().stream().map(TenantModel::getId).collect(Collectors.toList());
            List<Property> associatedProperties = propertyRepository.findAllByTenantIds(tenantIds);
            for (Property associatedProperty : associatedProperties) {
                mailService.sendEmailApplicantValidated(associatedProperty, tenantIds);
            }
        }
    }
}
