package fr.dossierfacile.api.front.aop;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class MethodLogAdvice {
    final private AuthenticationFacade authenticationFacade;

    @Around("@within(fr.dossierfacile.api.front.aop.annotation.MethodLog) || @annotation(fr.dossierfacile.api.front.aop.annotation.MethodLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt) {
                log.info(joinPoint.getSignature() + " has been executed in " + (System.currentTimeMillis() - start)
                        + "ms by " + authenticationFacade.getUserEmail()
                        + " with " + authenticationFacade.getKeycloakClientId());
            } else {
                log.info(joinPoint.getSignature() + " has been executed in " + (System.currentTimeMillis() - start)
                        + "ms by " + SecurityContextHolder.getContext().getAuthentication().getName());

            }
        }
    }
}
