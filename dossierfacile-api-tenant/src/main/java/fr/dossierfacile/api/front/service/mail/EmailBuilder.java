package fr.dossierfacile.api.front.service.mail;

import fr.dossierfacile.common.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailReplyTo;
import sibModel.SendSmtpEmailTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class EmailBuilder {

    private final Long templateId;
    private final Map<String, String> parameters = new HashMap<>();
    private String receiverEmail;
    private String receiverName;
    private String replyToEmail;

    static EmailBuilder fromTemplate(Long id) {
        return new EmailBuilder(id);
    }

    EmailBuilder to(User receiver) {
        this.receiverEmail = receiver.getEmail();
        this.receiverName = receiver.getFullName();
        return this;
    }

    EmailBuilder withParam(String key, String value) {
        this.parameters.put(key, value);
        return this;
    }

    EmailBuilder to(String email, String name) {
        this.receiverEmail = email;
        this.receiverName = name;
        return this;
    }

    EmailBuilder replyTo(String email) {
        this.replyToEmail = email;
        return this;
    }

    SendSmtpEmail build() {
        SendSmtpEmail email = new SendSmtpEmail();
        email.templateId(templateId);
        email.params(parameters);
        email.to(buildReceiver());
        email.replyTo(buildReplyTo());
        return email;
    }

    private List<SendSmtpEmailTo> buildReceiver() {
        SendSmtpEmailTo to = new SendSmtpEmailTo();
        to.setEmail(receiverEmail);
        if (StringUtils.isNotBlank(receiverName)) {
            to.setName(receiverName);
        }
        return Collections.singletonList(to);
    }

    private SendSmtpEmailReplyTo buildReplyTo() {
        SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
        replyTo.setEmail(replyToEmail);
        return replyTo;
    }
}