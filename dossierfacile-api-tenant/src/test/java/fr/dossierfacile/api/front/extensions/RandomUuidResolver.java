package fr.dossierfacile.api.front.extensions;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.util.UUID;

public class RandomUuidResolver extends TypeBasedParameterResolver<UUID> {

    @Override
    public UUID resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return UUID.randomUUID();
    }

}
