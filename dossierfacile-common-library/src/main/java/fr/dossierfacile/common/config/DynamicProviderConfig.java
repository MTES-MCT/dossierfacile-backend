package fr.dossierfacile.common.config;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class DynamicProviderConfig {
    private List<ObjectStorageProvider> providersConfig;

    private static List<ObjectStorageProvider> providers;

    @Value("#{'${storage.provider.list:OVH,THREEDS.OUTSCALE}'.split(',')}")
    public void setNameStatic(List<ObjectStorageProvider> providersConfig){
        providers = providersConfig;
    }

    @Bean
    public List<ObjectStorageProvider> getProviders() {
        return providers;
    }

    public void shift() {
        Collections.rotate(DynamicProviderConfig.providers, 1);
    }

}
