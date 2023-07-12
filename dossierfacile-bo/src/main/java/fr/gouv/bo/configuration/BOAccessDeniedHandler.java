package fr.gouv.bo.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BOAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        request.setAttribute("errorMessage",accessDeniedException.getMessage());
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value());
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, HttpStatus.FORBIDDEN.getReasonPhrase());
        request.getRequestDispatcher("/error").forward(request, response);
    }
}