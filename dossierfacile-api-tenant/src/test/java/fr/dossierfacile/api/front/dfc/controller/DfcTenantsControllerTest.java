package fr.dossierfacile.api.front.dfc.controller;

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

class DfcTenantsControllerTest {

    private MockMvc mvc;

    @BeforeEach
    public void setUp() {
        var controller = new DfcTenantsController(mock(ClientAuthenticationFacade.class), mock(TenantService.class), null, null);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void should_add_response_metadata() throws Exception {
        var request = get("/dfc/api/v1/tenants")
                .queryParam("after", "2020-01-31T10:30:00.000-05:00")
                .queryParam("limit", "10")
                .queryParam("includeDeleted", "true");

        String contentAsString = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        assertThat(contentAsString).isEqualToIgnoringNewLines("""
                {"data":[],"metadata":{"limit":10,"resultCount":0,"nextLink":"/dfc/api/v1/tenants?limit=10&after=2020-01-31T10:30&includeDeleted=true&includeRevoked=false"}}""");
    }

}