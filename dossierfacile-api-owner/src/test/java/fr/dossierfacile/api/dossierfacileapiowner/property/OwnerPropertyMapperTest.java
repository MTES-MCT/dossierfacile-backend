package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnerPropertyMapperTest {

    @Nested
    @DisplayName("Owner views property candidates list - token resolution from OWNER links")
    class ModificationsAfterMapping {

        @Test
        @DisplayName("should return active OWNER link token when both deleted and active links exist")
        void shouldReturnActiveTokenWhenDeletedLinksExist() {
            Property property = new Property();
            property.setId(1L);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            UUID deletedToken = UUID.randomUUID();
            UUID activeToken = UUID.randomUUID();

            ApartmentSharingLink deletedLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(deletedToken)
                    .linkType(ApartmentSharingLinkType.OWNER)
                    .propertyId(1L)
                    .deleted(true)
                    .expirationDate(LocalDateTime.now().minusDays(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            ApartmentSharingLink activeLink = ApartmentSharingLink.builder()
                    .id(2L)
                    .token(activeToken)
                    .linkType(ApartmentSharingLinkType.OWNER)
                    .propertyId(1L)
                    .deleted(false)
                    .expirationDate(LocalDateTime.now().plusDays(30))
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setApartmentSharingLinks(new ArrayList<>(List.of(deletedLink, activeLink)));

            PropertyApartmentSharing pas = new PropertyApartmentSharing();
            pas.setId(100L);
            pas.setApartmentSharing(apartmentSharing);
            property.setPropertiesApartmentSharing(List.of(pas));

            ApartmentSharingModel apartmentSharingModel = new ApartmentSharingModel();
            PropertyApartmentSharingModel pasModel = new PropertyApartmentSharingModel();
            pasModel.setId(100L);
            pasModel.setApartmentSharing(apartmentSharingModel);

            PropertyModel.PropertyModelBuilder builder = mock(PropertyModel.PropertyModelBuilder.class);
            PropertyModel propertyModel = PropertyModel.builder()
                    .propertiesApartmentSharing(List.of(pasModel))
                    .build();
            when(builder.build()).thenReturn(propertyModel);

            OwnerPropertyMapper mapper = new OwnerPropertyMapperImpl();
            mapper.modificationsAfterMapping(builder, property);

            assertThat(pasModel.getApartmentSharing().getToken()).isEqualTo(activeToken.toString());
        }

        @Test
        @DisplayName("should return empty token when all OWNER links are deleted")
        void shouldReturnEmptyTokenWhenAllLinksDeleted() {
            Property property = new Property();
            property.setId(1L);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            ApartmentSharingLink deletedLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(UUID.randomUUID())
                    .linkType(ApartmentSharingLinkType.OWNER)
                    .propertyId(1L)
                    .deleted(true)
                    .expirationDate(LocalDateTime.now().minusDays(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setApartmentSharingLinks(new ArrayList<>(List.of(deletedLink)));

            PropertyApartmentSharing pas = new PropertyApartmentSharing();
            pas.setId(100L);
            pas.setApartmentSharing(apartmentSharing);
            property.setPropertiesApartmentSharing(List.of(pas));

            ApartmentSharingModel apartmentSharingModel = new ApartmentSharingModel();
            PropertyApartmentSharingModel pasModel = new PropertyApartmentSharingModel();
            pasModel.setId(100L);
            pasModel.setApartmentSharing(apartmentSharingModel);

            PropertyModel.PropertyModelBuilder builder = mock(PropertyModel.PropertyModelBuilder.class);
            PropertyModel propertyModel = PropertyModel.builder()
                    .propertiesApartmentSharing(List.of(pasModel))
                    .build();
            when(builder.build()).thenReturn(propertyModel);

            OwnerPropertyMapper mapper = new OwnerPropertyMapperImpl();
            mapper.modificationsAfterMapping(builder, property);

            assertThat(pasModel.getApartmentSharing().getToken()).isEmpty();
        }

        @Test
        @DisplayName("should return empty token when OWNER link has expired")
        void shouldReturnEmptyTokenWhenLinkExpired() {
            Property property = new Property();
            property.setId(1L);

            ApartmentSharing apartmentSharing = new ApartmentSharing();
            apartmentSharing.setId(10L);

            ApartmentSharingLink expiredLink = ApartmentSharingLink.builder()
                    .id(1L)
                    .token(UUID.randomUUID())
                    .linkType(ApartmentSharingLinkType.OWNER)
                    .propertyId(1L)
                    .deleted(false)
                    .expirationDate(LocalDateTime.now().minusHours(1))
                    .apartmentSharing(apartmentSharing)
                    .build();

            apartmentSharing.setApartmentSharingLinks(new ArrayList<>(List.of(expiredLink)));

            PropertyApartmentSharing pas = new PropertyApartmentSharing();
            pas.setId(100L);
            pas.setApartmentSharing(apartmentSharing);
            property.setPropertiesApartmentSharing(List.of(pas));

            ApartmentSharingModel apartmentSharingModel = new ApartmentSharingModel();
            PropertyApartmentSharingModel pasModel = new PropertyApartmentSharingModel();
            pasModel.setId(100L);
            pasModel.setApartmentSharing(apartmentSharingModel);

            PropertyModel.PropertyModelBuilder builder = mock(PropertyModel.PropertyModelBuilder.class);
            PropertyModel propertyModel = PropertyModel.builder()
                    .propertiesApartmentSharing(List.of(pasModel))
                    .build();
            when(builder.build()).thenReturn(propertyModel);

            OwnerPropertyMapper mapper = new OwnerPropertyMapperImpl();
            mapper.modificationsAfterMapping(builder, property);

            assertThat(pasModel.getApartmentSharing().getToken()).isEmpty();
        }
    }
}
