package fr.dossierfacile.api.front.service.mail;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SendinblueEmailTemplateIds {

    @Value("${sendinblue.template.id.welcome}")
    private Long welcomeEmail;

    @Value("${sendinblue.template.id.new.password}")
    private Long newPasswordEmail;

    @Value("${sendinblue.template.id.invitation.couple}")
    private Long coupleApplicationEmail;

    @Value("${sendinblue.template.id.invitation.group}")
    private Long groupApplicationEmail;

    @Value("${sendinblue.template.id.account.deleted}")
    private Long accountDeletedEmail;

    @Value("${sendinblue.template.id.account.completed}")
    private Long accountCompletedEmail;

    @Value("${sendinblue.template.id.account.email.validation.reminder}")
    private Long accountNotYetValidatedEmail;

    @Value("${sendinblue.template.id.account.incomplete.reminder}")
    private Long accountNotYetCompletedEmail;

    @Value("${sendinblue.template.id.account.declined.reminder}")
    private Long accountIsStillDeclinedEmail;

    @Value("${sendinblue.template.id.account.satisf.not.assoc.to.partners}")
    private Long tenantNotAssociatedToPartnersAndValidatedEmail;

    @Value("${sendinblue.template.id.account.satisf.yes.assoc.to.partners}")
    private Long tenantAssociatedToPartnersAndValidatedEmail;

    @Value("${sendinblue.template.id.first.warning.for.deletion.of.documents}")
    private Long firstWarningBeforeDocumentsDeletionEmail;

    @Value("${sendinblue.template.id.second.warning.for.deletion.of.documents}")
    private Long secondWarningBeforeDocumentsDeletionEmail;

    @Value("${sendinblue.template.id.contact.support}")
    private Long contactSupportEmail;

    @Value("${sendinblue.template.id.welcome.partner}")
    private Long welcomePartnerEmail;

}