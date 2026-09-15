package fr.gouv.bo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.constants.PartnerConstants;
import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.dto.UserApiDTO;
import fr.gouv.bo.service.UserApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOUserApiControllerTest {

    @Mock
    private UserApiService userApiService;

    private BOUserApiController controller;

    @BeforeEach
    void setUp() {
        controller = new BOUserApiController(userApiService, new ObjectMapper());
    }

    private UserApiDTO partner(String name, boolean completedStatusSupported) {
        UserApiDTO dto = new UserApiDTO();
        dto.setId(1L);
        dto.setName(name);
        dto.setEmail("partner@example.org");
        dto.setCompletedStatusSupported(completedStatusSupported);
        return dto;
    }

    private BindingResult bindingResultFor(UserApiDTO dto) {
        return new BeanPropertyBindingResult(dto, "userApiDTO");
    }

    private void persistedCompletedStatus(String name, boolean completedStatusSupported) {
        when(userApiService.findById(1L)).thenReturn(
                UserApi.builder().id(1L).name(name).completedStatusSupported(completedStatusSupported).build());
    }

    @Test
    void update_savesAPartnerThatIntegratedCompleted() {
        UserApiDTO dto = partner("dfconnect-ics", true);
        persistedCompletedStatus("dfconnect-ics", false);

        String view = controller.update(1L, dto, bindingResultFor(dto));

        assertThat(view).isEqualTo("redirect:/bo/userApi");
        verify(userApiService).save(dto);
    }

    @Test
    void update_refusesCompletedStatusForTheOwnerPartner() {
        UserApiDTO dto = partner(PartnerConstants.DF_OWNER_NAME, true);
        persistedCompletedStatus(PartnerConstants.DF_OWNER_NAME, false);
        BindingResult result = bindingResultFor(dto);

        String view = controller.update(1L, dto, result);

        assertThat(view).isEqualTo("bo/user-api-edit");
        assertThat(result.getFieldError("completedStatusSupported")).isNotNull();
        verify(userApiService, never()).save(any());
    }

    @Test
    void create_refusesCompletedStatusForTheOwnerPartner() {
        UserApiDTO dto = partner(PartnerConstants.DF_OWNER_NAME, true);

        controller.create(dto, bindingResultFor(dto));

        verify(userApiService, never()).create(any());
    }

    @Test
    void update_acceptsTheOwnerPartnerWithoutCompletedStatus() {
        UserApiDTO dto = partner(PartnerConstants.DF_OWNER_NAME, false);
        persistedCompletedStatus(PartnerConstants.DF_OWNER_NAME, false);

        controller.update(1L, dto, bindingResultFor(dto));

        verify(userApiService).save(dto);
    }
}
