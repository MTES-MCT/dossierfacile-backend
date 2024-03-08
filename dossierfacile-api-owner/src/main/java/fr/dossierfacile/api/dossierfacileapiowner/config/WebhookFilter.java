package fr.dossierfacile.api.dossierfacileapiowner.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * DossierFacile will send callback request at change on tenant status.
 * This filter check if callback request contain a valid x-api-key.
 */
public class WebhookFilter extends OncePerRequestFilter {
    private final String token;
    private final String headerName;

    private final RequestMatcher uriMatcher =
            new AntPathRequestMatcher("/webhook/**", HttpMethod.POST.name());

    public WebhookFilter(String token, String headerName) {
        this.token = token;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        String apiKey = getApiKey(request);
        if (apiKey != null && !apiKey.equals(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid API Key");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String getApiKey(HttpServletRequest httpRequest) {
        String apiKey = httpRequest.getHeader(headerName);
        if (apiKey != null) {
            apiKey = apiKey.trim();
        }
        return apiKey;
    }

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        RequestMatcher matcher = new NegatedRequestMatcher(uriMatcher);
        return matcher.matches(request);
    }
}