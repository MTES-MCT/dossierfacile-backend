package fr.dossierfacile.parameterizedtest;

import lombok.Getter;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.function.UnaryOperator;

@Getter
public class ControllerParameter<T> {

    private T parameterData;
    private int status;
    private RequestPostProcessor requestPostProcessor;
    private UnaryOperator<Void> setupMock;
    private List<ResultMatcher> resultMatchers;
    private UnaryOperator<Void> mockVerifications = null;

    public ControllerParameter(
            T parameterData,
            int status,
            RequestPostProcessor requestPostProcessor,
            UnaryOperator<Void> setupMock,
            List<ResultMatcher> resultMatchers
    ) {
        this.parameterData = parameterData;
        this.status = status;
        this.requestPostProcessor = requestPostProcessor;
        this.setupMock = setupMock;
        this.resultMatchers = resultMatchers;
    }

    public ControllerParameter(
            T parameterData,
            int status,
            RequestPostProcessor requestPostProcessor,
            UnaryOperator<Void> setupMock,
            List<ResultMatcher> resultMatchers,
            UnaryOperator<Void> mockVerifications
    ) {
        this.parameterData = parameterData;
        this.status = status;
        this.requestPostProcessor = requestPostProcessor;
        this.setupMock = setupMock;
        this.resultMatchers = resultMatchers;
        this.mockVerifications = mockVerifications;
    }
}
