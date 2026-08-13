package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApartmentSharingModelMapperTest {

    private final ApartmentSharingModelMapper mapper = Mappers.getMapper(ApartmentSharingModelMapper.class);

    private ApartmentSharing apartmentSharingWithStatus(TenantFileStatus status) {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .status(status)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setTenants(List.of(tenant));
        tenant.setApartmentSharing(apartmentSharing);
        return apartmentSharing;
    }

    // The COMPLETED status must never reach an owner facing DTO: this test protects
    // the defensive masking from being removed as dead code
    @Test
    void should_mask_completed_status_for_owners() {
        ApartmentSharingModel model = mapper.apartmentSharingToApartmentSharingModel(
                apartmentSharingWithStatus(TenantFileStatus.COMPLETED));

        assertThat(model.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS.name());
        assertThat(model.getTenants().getFirst().getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
    }

    @Test
    void should_keep_other_statuses_unchanged() {
        ApartmentSharingModel model = mapper.apartmentSharingToApartmentSharingModel(
                apartmentSharingWithStatus(TenantFileStatus.VALIDATED));

        assertThat(model.getStatus()).isEqualTo(TenantFileStatus.VALIDATED.name());
        assertThat(model.getTenants().getFirst().getStatus()).isEqualTo(TenantFileStatus.VALIDATED);
    }
}
