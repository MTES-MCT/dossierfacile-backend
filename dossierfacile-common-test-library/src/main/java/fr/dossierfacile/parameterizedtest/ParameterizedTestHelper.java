package fr.dossierfacile.parameterizedtest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.function.Function;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ParameterizedTestHelper {
    public static <T> void runControllerTest(
            MockMvc mockMvc,
            MockHttpServletRequestBuilder mockMvcRequestBuilder,
            ControllerParameter<T> parameter
    ) throws Exception {
        if (parameter.setupMock != null) {
            parameter.setupMock.apply(null);
        }

        if (parameter.requestPostProcessor != null) {
            mockMvcRequestBuilder.with(parameter.requestPostProcessor);
        }

        mockMvc.perform(mockMvcRequestBuilder)
                .andDo(print())
                .andExpect(status().is(parameter.status))
                .andExpectAll(parameter.resultMatchers.toArray(ResultMatcher[]::new));
    }
}
