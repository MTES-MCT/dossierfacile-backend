package fr.gouv.owner.configuration;

import fr.gouv.owner.register_owner.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegisterOwnerConfig {

    private BeanFactory beanFactory;

    public RegisterOwnerConfig(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public ServiceLocatorFactoryBean myFactoryLocator() {
        final ServiceLocatorFactoryBean locator = new ServiceLocatorFactoryBean();
        locator.setServiceLocatorInterface(RegisterOwnerFactory.class);
        locator.setBeanFactory(beanFactory);
        return locator;
    }

    @Bean
    public RegisterOwnerFactory registerOwnerFactory() {
        final ServiceLocatorFactoryBean locator = myFactoryLocator();
        locator.afterPropertiesSet();
        return (RegisterOwnerFactory) locator.getObject();
    }

    @Bean(name = "step1")
    public SaveStep step1() {
        return new SaveStep1();
    }

    @Bean(name = "step2")
    public SaveStep step2() {
        return new SaveStep2();
    }

    @Bean(name = "step3")
    public SaveStep step3() {
        return new SaveStep3();
    }
}
