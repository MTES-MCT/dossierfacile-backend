package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapperTest {

    @Nested
    @DisplayName("Owner views single property detail - token resolution from property links")
    class GetToken {

        private final PropertyMapper mapper = new PropertyMapperImpl();

        @Test
        @DisplayName("should return active link token when both deleted and active links exist")
        void shouldReturnActiveTokenWhenDeletedLinksExist() {
            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            UUID deletedToken = UUID.randomUUID();
            UUID activeToken = UUID.randomUUID();

            ApartmentSharingLink deletedLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(deletedToken)
                    .deleted(true)
                    .expirationDate(LocalDateTime.now().minusDays(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            ApartmentSharingLink activeLink = ApartmentSharingLink.builder()
                    .id(2L)
                    .token(activeToken)
                    .deleted(false)
                    .expirationDate(LocalDateTime.now().plusDays(30))
                    .apartmentSharing(apartmentSharing)
                    .build();

            Property property = new Property();
            property.setApartmentSharingLinks(new ArrayList<>(List.of(deletedLink, activeLink)));

            String result = mapper.getToken(apartmentSharing, property);

            assertThat(result).isEqualTo(activeToken.toString());
        }

        @Test
        @DisplayName("should return null when all links are deleted")
        void shouldReturnNullWhenAllLinksDeleted() {
            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            ApartmentSharingLink deletedLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(UUID.randomUUID())
                    .deleted(true)
                    .expirationDate(LocalDateTime.now().minusDays(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            Property property = new Property();
            property.setApartmentSharingLinks(new ArrayList<>(List.of(deletedLink)));

            String result = mapper.getToken(apartmentSharing, property);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when link has expired")
        void shouldReturnNullWhenLinkExpired() {
            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            ApartmentSharingLink expiredLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(UUID.randomUUID())
                    .deleted(false)
                    .expirationDate(LocalDateTime.now().minusHours(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            Property property = new Property();
            property.setApartmentSharingLinks(new ArrayList<>(List.of(expiredLink)));

            String result = mapper.getToken(apartmentSharing, property);

            assertThat(result).isNull();
        }
    }
}
