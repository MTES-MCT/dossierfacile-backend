package fr.dossierfacile.parameterizedtest;

import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.function.Function;

public class ControllerParameter<T> {

    public T parameterData;
    public int status;
    public RequestPostProcessor requestPostProcessor;
    public Function<Void, Void> setupMock;
    public List<ResultMatcher> resultMatchers;
    public Function<Void, Void> mockVerifications = null;

    public ControllerParameter(
            T parameterData,
            int status,
            RequestPostProcessor requestPostProcessor,
            Function<Void, Void> setupMock,
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
            Function<Void, Void> setupMock,
            List<ResultMatcher> resultMatchers,
            Function<Void, Void> mockVerifications
    ) {
        this.parameterData = parameterData;
        this.status = status;
        this.requestPostProcessor = requestPostProcessor;
        this.setupMock = setupMock;
        this.resultMatchers = resultMatchers;
        this.mockVerifications = mockVerifications;
    }
}
