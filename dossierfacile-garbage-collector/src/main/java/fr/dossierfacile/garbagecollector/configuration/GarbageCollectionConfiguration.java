package fr.dossierfacile.garbagecollector.configuration;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.garbagecollector.repo.garbagecollection.GarbageCollectionDetailsRepository;
import fr.dossierfacile.garbagecollector.service.OvhServiceImpl;
import fr.dossierfacile.garbagecollector.service.ScheduledGarbageCollectionService;
import fr.dossierfacile.garbagecollector.service.interfaces.StorageProviderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class GarbageCollectionConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "garbage-collection.enabled",
            havingValue = "true"
    )
    ScheduledGarbageCollectionService scheduledGarbageCollectionService(
            GarbageCollectionDetailsRepository garbageCollectionDetailsRepository,
            StorageFileRepository storageFileRepository,
            @Qualifier("ovhStorageProviderService") StorageProviderService ovhService,
            @Value("${garbage-collection.objects-by-iteration:100}") int numberOfObjectsToCheckByIteration
    ) {
        Map<ObjectStorageProvider, StorageProviderService> storageProviderServices = Map.of(
                ObjectStorageProvider.OVH, ovhService
        );
        return new ScheduledGarbageCollectionService(
                garbageCollectionDetailsRepository,
                storageFileRepository,
                storageProviderServices,
                numberOfObjectsToCheckByIteration);
    }

}
