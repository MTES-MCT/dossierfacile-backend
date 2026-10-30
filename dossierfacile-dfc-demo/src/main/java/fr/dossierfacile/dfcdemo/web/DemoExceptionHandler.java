package fr.dossierfacile.dfcdemo.web;

import fr.dossierfacile.dfcdemo.oidc.DfcApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Shows the failing step and the raw upstream answer instead of a generic error page. */
@Slf4j
@ControllerAdvice
public class DemoExceptionHandler {

    @ExceptionHandler(DfcApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handle(DfcApiException e, Model model) {
        log.warn("DFC flow error at step {}: status={}", e.getStep(), e.getStatus());
        model.addAttribute("step", e.getStep());
        model.addAttribute("status", e.getStatus());
        model.addAttribute("responseBody", e.getResponseBody());
        return "error";
    }
}
