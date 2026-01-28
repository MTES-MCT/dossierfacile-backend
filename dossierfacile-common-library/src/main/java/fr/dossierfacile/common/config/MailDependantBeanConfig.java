package fr.dossierfacile.common.config;

import brevoApi.TransactionalEmailsApi;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.repository.UserApiRepository;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import fr.dossierfacile.common.service.MailCommonServiceImpl;
import fr.dossierfacile.common.service.interfaces.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailDependantBeanConfig {

    // We need to set proxyBeanMethods to false because by default a proxy around the configuration class is created
    // This proxy will introspect the declaration methods and the annotations.
    // But some project does not have Brevo dependency inside the classPath so TransactionEmailApi will throw a class not found exception
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "brevoApi.TransactionalEmailsApi")
    public static class BrevoMailConfig {

        @Bean
        @ConditionalOnBean(TransactionalEmailsApi.class)
        @ConditionalOnProperty(name = "brevo.enabled", havingValue = "true", matchIfMissing = true)
        public MailCommonService mailCommonService(TransactionalEmailsApi transactionalEmailsApi) {
            return new MailCommonServiceImpl(transactionalEmailsApi);
        }
    }

    @Bean
    @ConditionalOnClass(MailCommonService.class)
    @ConditionalOnBean(MailCommonService.class)
    public ApartmentSharingLinkService apartmentSharingLinkService(
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            LinkLogService linkLogService,
            TenantCommonRepository tenantCommonRepository,
            UserApiRepository userApiRepository,
            TenantUserApiRepository tenantUserApiRepository,
            PartnerCallBackService partnerCallBackService,
            KeycloakCommonService keycloakCommonService,
            MailCommonService mailCommonService,
            LogService logService
    ) {
        return new ApartmentSharingLinkService(
                apartmentSharingLinkRepository,
                linkLogService,
                tenantCommonRepository,
                userApiRepository,
                tenantUserApiRepository,
                partnerCallBackService,
                keycloakCommonService,
                mailCommonService,
                logService
        );
    }
}
