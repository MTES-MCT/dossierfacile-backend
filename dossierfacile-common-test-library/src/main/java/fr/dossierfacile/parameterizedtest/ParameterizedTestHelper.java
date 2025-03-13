package fr.dossierfacile.parameterizedtest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParameterizedTestHelper {

    private ParameterizedTestHelper() {}

    public static <T> void runControllerTest(
            MockMvc mockMvc,
            MockHttpServletRequestBuilder mockMvcRequestBuilder,
            ControllerParameter<T> parameter
    ) throws Exception {
        if (parameter.getSetupMock() != null) {
            parameter.getSetupMock().apply(null);
        }

        if (parameter.getRequestPostProcessor() != null) {
            mockMvcRequestBuilder.with(parameter.getRequestPostProcessor());
        }

        mockMvc.perform(mockMvcRequestBuilder)
                .andDo(print())
                .andExpect(status().is(parameter.getStatus()))
                .andExpectAll(parameter.getResultMatchers().toArray(ResultMatcher[]::new));

        if (parameter.getMockVerifications() != null) {
            parameter.getMockVerifications().apply(null);
        }
    }
}
