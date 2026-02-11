package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiPartnerTenantControllerTest {

    private MockMvc mvc;

    @BeforeEach
    public void setUp() {
        var controller = new ApiPartnerTenantController(mock(ClientAuthenticationFacade.class), mock(TenantService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void should_add_response_metadata() throws Exception {
        var request = get("/api-partner/tenant")
                .queryParam("after", "2020-01-31T10:30:00.000-05:00")
                .queryParam("limit", "10")
                .queryParam("includeDeleted", "true");

        String contentAsString = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        assertThat(contentAsString).isEqualToIgnoringNewLines("""
                {"data":[],"metadata":{"limit":10,"resultCount":0,"nextLink":"/api-partner/tenant?limit=10&orderBy=LAST_UPDATE_DATE&after=2020-01-31T10:30&includeDeleted=true&includeRevoked=false"}}""");
    }

}