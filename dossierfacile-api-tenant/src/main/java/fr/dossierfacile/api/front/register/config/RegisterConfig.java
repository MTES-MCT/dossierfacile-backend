package fr.dossierfacile.api.front.register.config;

import fr.dossierfacile.api.front.register.RegisterFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegisterConfig {

    private final BeanFactory beanFactory;

    public RegisterConfig(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    private ServiceLocatorFactoryBean myFactoryLocator() {
        final ServiceLocatorFactoryBean locator = new ServiceLocatorFactoryBean();
        locator.setServiceLocatorInterface(RegisterFactory.class);
        locator.setBeanFactory(beanFactory);
        return locator;
    }

    @Bean
    public RegisterFactory registerTenantFactory() {
        final ServiceLocatorFactoryBean locator = myFactoryLocator();
        locator.afterPropertiesSet();
        return (RegisterFactory) locator.getObject();
    }
}
