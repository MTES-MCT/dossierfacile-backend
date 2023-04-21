package fr.dossierfacile.api.front.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MailSentLimitException extends RuntimeException {
    public MailSentLimitException() {
        super("Can not send more than 10 mails per day");
    }
}
