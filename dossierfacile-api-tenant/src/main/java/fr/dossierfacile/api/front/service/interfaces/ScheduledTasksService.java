package fr.dossierfacile.api.front.service.interfaces;

public interface ScheduledTasksService {

    void emailAccountValidationReminder();

    void accountCompletionReminder();

    void accountDeclinationReminder();

    void satisfactionEmails();

    void updateOperationAccessTokenStatus();
}
