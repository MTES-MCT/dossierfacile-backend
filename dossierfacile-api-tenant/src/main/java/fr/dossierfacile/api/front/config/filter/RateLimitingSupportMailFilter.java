package fr.dossierfacile.api.front.config.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

@Component
@WebFilter("/api/support/email")
public class RateLimitingSupportMailFilter extends AbstractRateLimitingFilter implements Filter {

    @Value("${ratelimit.support.email.capacity}")
    private int registerCapacity;
    @Value("${ratelimit.support.email.refill.delay.in.minute}")
    private int refillDelayInMinute;

    @Override
    protected int getRefillDelayInMinute() {
        return refillDelayInMinute;
    }

    @Override
    protected int getRegisterCapacity() {
        return registerCapacity;
    }
}
