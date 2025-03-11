package fr.dossierfacile.api.dossierfacileapiowner.aop;

import fr.dossierfacile.common.utils.LoggerUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SensitiveRequestAspect {

    @Before(value = "@annotation(annotation)")
    public void doBefore(JoinPoint joinPoint, SensitiveRequest annotation) {
        if (annotation.isBodySensitive()) {
            LoggerUtil.setRequestParamSensitive();
        }
        if (annotation.isResponseSensitive()) {
            LoggerUtil.setResponseParamSensitive();
        }
    }

}
