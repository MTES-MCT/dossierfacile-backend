package fr.dossierfacile.scheduler.configuration;

import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.scheduler.repo.garbagecollection.GarbageCollectionDetailsRepository;
import fr.dossierfacile.scheduler.service.ScheduledGarbageCollectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            List<FileStorageProviderService> fileStorageProviderServices,
            @Value("${garbage-collection.objects-by-iteration:100}") int numberOfObjectsToCheckByIteration
    ) {
        var storageProviderServicesMap = fileStorageProviderServices.stream()
                        .collect(Collectors.toMap(FileStorageProviderService::getProvider, Function.identity()));
        return new ScheduledGarbageCollectionService(
                garbageCollectionDetailsRepository,
                storageFileRepository,
                storageProviderServicesMap,
                numberOfObjectsToCheckByIteration);
    }

}
