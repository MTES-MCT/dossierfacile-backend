package fr.dossierfacile.scheduler.service.interfaces;

import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.User;

public interface MailService {

    void sendEmailFirstWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken);

    void sendEmailSecondWarningForDeletionOfDocuments(User user, ConfirmationToken confirmationToken);

}
