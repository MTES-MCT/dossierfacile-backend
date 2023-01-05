package fr.dossierfacile.common.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @ParameterizedTest
    @CsvSource({
            "Jean, Dupont, , Jean Dupont",
            "Jean, Dupont, Martin, Jean Martin",
            "Jean, , Martin, Jean Martin",
            ", Dupont, , ''",
            ", , , ''",
            "'', '', '', ''",
    })
    void should_return_full_name(String firstName, String lastName, String preferredName, String expectedFullName) {
        User user = new Tenant();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPreferredName(preferredName);

        assertThat(user.getFullName()).isEqualTo(expectedFullName);
    }

}