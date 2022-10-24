package fr.dossierfacile.api.front.service.mail;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.UserType;
import org.junit.jupiter.api.Test;
import sibModel.SendSmtpEmail;

import static fr.dossierfacile.api.front.assertions.TenantAssertions.assertThat;

class EmailBuilderTest {

    @Test
    void should_build_email() {
        User user = new User(UserType.TENANT, "John", "Doe", "john@doe.io");

        SendSmtpEmail email = EmailBuilder.fromTemplate(123L)
                .withParam("someData", "abc")
                .withParam("otherData", "def")
                .replyTo("reply@test.com")
                .to(user)
                .build();

        assertThat(email).hasTemplateId(123L)
                .hasParameter("someData", "abc")
                .hasParameter("otherData", "def")
                .hasReplyTo("reply@test.com")
                .hasReceiver("john@doe.io", "John Doe");
    }

}