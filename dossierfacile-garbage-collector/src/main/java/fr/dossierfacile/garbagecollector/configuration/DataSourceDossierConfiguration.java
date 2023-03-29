package fr.dossierfacile.garbagecollector.configuration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableTransactionManagement
@PropertySource({"classpath:application.properties"})
@EnableJpaRepositories(entityManagerFactoryRef = "dossierEntityManager", transactionManagerRef = "dossierTransactionManager",
        basePackages = {"fr.dossierfacile.garbagecollector.repo.guarantor","fr.dossierfacile.garbagecollector.repo.document", "fr.dossierfacile.garbagecollector.repo.file", "fr.dossierfacile.garbagecollector.repo.apartment", "fr.dossierfacile.common"})
public class DataSourceDossierConfiguration {
    @Autowired
    private Environment env;

    @Bean
    public LocalContainerEntityManagerFactoryBean dossierEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(productDataSource());
        em.setPackagesToScan(
                "fr.dossierfacile.garbagecollector.repo.guarantor", "fr.dossierfacile.garbagecollector.model.document", "fr.dossierfacile.garbagecollector.model.file", "fr.dossierfacile.garbagecollector.model.apartment", "fr.dossierfacile.common");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto",
                env.getProperty("hibernate.hbm2ddl.auto"));
        properties.put("hibernate.dialect",
                env.getProperty("hibernate.dialect"));
        properties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
        properties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public DataSource productDataSource() {

        DriverManagerDataSource dataSource
                = new DriverManagerDataSource();
        dataSource.setDriverClassName(
                env.getProperty("postgre2.datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("postgre2.datasource.url"));
        dataSource.setUsername(env.getProperty("postgre2.datasource.username"));
        dataSource.setPassword(env.getProperty("postgre2.datasource.password"));

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager dossierTransactionManager() {

        JpaTransactionManager transactionManager
                = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
                dossierEntityManager().getObject());
        return transactionManager;
    }
}
