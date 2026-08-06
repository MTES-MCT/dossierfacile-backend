package fr.dossierfacile.api.front.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation permettant d'injecter directement l'identifiant Keycloak (le "sub" du JWT)
 * en tant que paramètre de méthode dans les contrôleurs.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeycloakId {
}
