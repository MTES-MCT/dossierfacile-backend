package fr.dossierfacile.api.front.assertions;

import org.assertj.core.api.AbstractAssert;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailTo;

public class SendSmtpEmailAssert extends AbstractAssert<SendSmtpEmailAssert, SendSmtpEmail> {

    public SendSmtpEmailAssert(SendSmtpEmail actual) {
        super(actual, SendSmtpEmailAssert.class);
    }

    public SendSmtpEmailAssert hasTemplateId(Long id) {
        isNotNull();
        Long actualTemplateId = actual.getTemplateId();
        if (!actualTemplateId.equals(id)) {
            failWithMessage("Expected template id to be %s, but was %s",
                    id, actualTemplateId);
        }
        return this;
    }

    public SendSmtpEmailAssert hasParameter(String key, String value) {
        isNotNull();
        if (!actual.getParams().toString().contains(key + "=" + value)) {
            failWithMessage("Expected parameters to contain %s=%s, but were %s",
                    key, value, actual.getParams().toString());
        }
        return this;
    }

    public SendSmtpEmailAssert hasReceiver(String email, String name) {
        isNotNull();
        if (actual.getTo().isEmpty()) {
            failWithMessage("Expected receiver %s but found none", email);
        }
        SendSmtpEmailTo receiver = actual.getTo().get(0);
        if (!receiver.getEmail().equals(email)) {
            failWithMessage("Expected receiver to have email %s, but was %s",
                    email, receiver.getEmail());
        }
        if (!receiver.getName().equals(name)) {
            failWithMessage("Expected receiver to have name %s, but was %s",
                    name, receiver.getName());
        }
        return this;
    }

    public SendSmtpEmailAssert hasReplyTo(String email) {
        isNotNull();
        String actualReplyTo = actual.getReplyTo().getEmail();
        if (!actualReplyTo.equals(email)) {
            failWithMessage("Expected replyTo to be %s, but was %s",
                    email, actualReplyTo);
        }
        return this;
    }

}
