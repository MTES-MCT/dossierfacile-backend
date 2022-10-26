package fr.dossierfacile.common.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void should_display_first_and_last_name() {
        User user = new User();
        user.setFirstName("Jean");
        user.setLastName("Dupont");

        assertThat(user.getFullName()).isEqualTo("Jean Dupont");
    }

    @Nested
    class PreferredName {

        @Test
        void should_choose_preferred_name_over_last_name() {
            User user = new User();
            user.setLastName("Dupont");
            user.setPreferredName("Martin");

            assertThat(user.getPreferredLastName()).isEqualTo("Martin");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void should_default_to_last_name(String noPreferredName) {
            User user = new User();
            user.setLastName("Dupont");
            user.setPreferredName(noPreferredName);

            assertThat(user.getPreferredLastName()).isEqualTo("Dupont");
        }

    }

}