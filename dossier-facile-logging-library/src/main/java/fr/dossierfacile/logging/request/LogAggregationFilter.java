package fr.dossierfacile.logging.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.logging.appender.CustomAppender;
import fr.dossierfacile.logging.model.LogModel;
import fr.dossierfacile.logging.util.LoggerUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "dossierfacile.logging.request.aggregator", havingValue = "true")
public class LogAggregationFilter extends OncePerRequestFilter {
    private Logger rootLogger;
    private CustomAppender customAppender;

    final ObjectMapper objectMapper;

    public LogAggregationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        customAppender = CustomAppender.initCustomAppender(LoggerUtil.REQUEST_ID);
        rootLogger.addAppender(customAppender);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var mdcRequestId = LoggerUtil.getRequestId();
        if (mdcRequestId == null) {
            LoggerUtil.prepareMDCForHttpRequest(request, Collections.emptyMap());
        }

        // We need to use this wrapper because we can only read the request body input stream once.
        // So if we read it inside the filter, the controller will not be able to read it.
        ContentCachingRequestWrapper requestWrapped = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapped = new ContentCachingResponseWrapper(response);

        String requestId = LoggerUtil.getRequestId();
        boolean requestFinishedInError = false;

        //Save the time before the request is processed
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapped, responseWrapped);
        }
        catch (Exception e) {
            requestFinishedInError = true;
            logger.error("Uncaught exception during the request : ", e);
            throw e;
        }
        finally
        {
            LoggerUtil.addRequestStatusToMdc(response.getStatus());
            List<LogModel> logs = customAppender.getLogsForUniqueIdentifier(requestId);
            String logMessages = objectMapper.writeValueAsString(logs);

            if (!LoggerUtil.isRequestParamSensitive()) {
                var requestParameters = request.getQueryString();
                if (requestParameters != null && !requestParameters.isEmpty()) {
                    LoggerUtil.addRequestParams(requestParameters);
                }
                if (request.getContentType() != null && request.getContentType().contains("application/json")) {
                    String requestBody = getRequestBody(requestWrapped);
                    LoggerUtil.addRequestBody(requestBody);
                }

                if (request.getContentType() != null && request.getContentType().contains("multipart/form-data")) {
                    var requestParts = request.getParts();
                    var requestPartString = requestParts.stream().map(Part::getName).collect(Collectors.joining(","));
                    LoggerUtil.addRequestFormParameters(requestPartString);
                }
            }

            var responseType = response.getContentType();
            if (responseType != null) {
                LoggerUtil.addResponseContentType(responseType);
                if (!LoggerUtil.isResponseParamSensitive() && responseType.contains("application/json")) {
                    var responseBody = getResponseBody(responseWrapped);
                    LoggerUtil.addResponseBody(responseBody);
                }
            }

            long responseTime = System.currentTimeMillis() - startTime;
            var finalStatus = response.getStatus();
            if (requestFinishedInError && finalStatus == HttpServletResponse.SC_OK) {
                finalStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                LoggerUtil.addRequestStatusToMdc(finalStatus);

            }
            String enrichedLogs = String.format(
                    "Request completed: URI:%s, Method:%s, Status:%d, response time: %s",
                    request.getRequestURI(),
                    request.getMethod(),
                    finalStatus,
                    responseTime
            );

            LoggerUtil.addLogs(logMessages);
            LoggerUtil.addExecutionTime(responseTime);

            Level logLevel = LoggerUtil.getLogLevel(logs);
            LoggerUtil.sendEnrichedLogs(rootLogger, logLevel, enrichedLogs);

            customAppender.clearLogsForRequest(requestId);
            // We need to copy back the response body to the original response
            responseWrapped.copyBodyToResponse();
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        return new String(request.getContentAsByteArray());
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        return new String(response.getContentAsByteArray());
    }
}
