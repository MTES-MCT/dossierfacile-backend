package fr.dossierfacile.common.config;

/**
 * Defines allowed API versions
 */
public interface ApiVersion {
    static Class<?> getVersionClass(Integer version) {
        return switch (version) {
            case 4 -> ApiVersion.V4.class;
            default -> throw new IllegalArgumentException("This version is not managed");
        };
    }


    interface V4 {
        static boolean is(Integer v) {
            return v == 4;
        }
    }
}
