package fr.dossierfacile.api.pdfgenerator.util.parameterresolvers;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.util.List;

public class ApartmentSharingResolver extends TypeBasedParameterResolver<ApartmentSharing> {

    @Override
    public ApartmentSharing resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return buildApartmentSharing();
    }

    private ApartmentSharing buildApartmentSharing() {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(1L);
        apartmentSharing.setApplicationType(ApplicationType.ALONE);

        Tenant tenant = TenantResolver.buildTenant();
        apartmentSharing.setTenants(List.of(tenant));

        return apartmentSharing;
    }

}
