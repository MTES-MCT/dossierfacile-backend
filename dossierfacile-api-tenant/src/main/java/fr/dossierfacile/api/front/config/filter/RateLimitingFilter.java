package fr.dossierfacile.api.front.config.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
@WebFilter("/api/register/account")
public class RateLimitingFilter extends AbstractRateLimitingFilter implements Filter {
    @Value("${ratelimit.register.capacity}")
    private int registerCapacity;
    @Value("${ratelimit.register.refill.delay.in.minute}")
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
