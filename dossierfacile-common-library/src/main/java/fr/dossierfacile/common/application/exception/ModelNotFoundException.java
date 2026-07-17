package fr.dossierfacile.common.application.exception;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(Class<?> clazz, Object id) {
        super(clazz.getName() + " not found with id: " + id);
    }
}
