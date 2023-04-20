package fr.dossierfacile.api.front.aop;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class MethodLogExecutionTimeAdvice {
    @Around("@within(fr.dossierfacile.api.front.aop.annotation.MethodLogTime) || @annotation(fr.dossierfacile.api.front.aop.annotation.MethodLogTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            log.info(joinPoint.getSignature() + " has been executed in " + (System.currentTimeMillis() - start) + "ms ");
        }
    }
}
