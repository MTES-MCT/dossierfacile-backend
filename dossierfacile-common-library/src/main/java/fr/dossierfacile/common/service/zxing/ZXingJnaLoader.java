package fr.dossierfacile.common.service.zxing;

import com.sun.jna.NativeLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;

public final class ZXingJnaLoader {

    private static volatile boolean loaded = false;

    private ZXingJnaLoader() {}

    public static synchronized void load() {
        if (loaded) return;

        String os = detectOs();
        String arch = detectArch();
        String platform = os + "-" + arch;

        String wrapperName = switch (os) {
            case "linux"  -> "libzxing_jna.so";
            case "macos"  -> "libzxing_jna.dylib";
            case "windows"-> "zxing_jna.dll";
            default -> throw new IllegalStateException("Unsupported OS: " + os);
        };
        String coreName = switch (os) {
            case "linux"  -> "libZXing.so.3";
            case "macos"  -> "libZXing.3.dylib";
            case "windows"-> "ZXing.dll";
            default -> throw new IllegalStateException("Unsupported OS: " + os);
        };

        String base = "/natives/" + platform + "/";
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory("zxing_jna_");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp dir for native libs", e);
        }

        Path coreOut    = tmpDir.resolve(coreName);
        Path wrapperOut = tmpDir.resolve(wrapperName);

        extract(base + coreName, coreOut);
        extract(base + wrapperName, wrapperOut);

        // JNA: ajoute le dossier d’extraction dans le chemin de recherche de "zxing_jna"
        NativeLibrary.addSearchPath("zxing_jna", tmpDir.toAbsolutePath().toString());
        // Optionnel : visibilité pour d’autres chargeurs
        System.setProperty("jna.library.path", tmpDir.toAbsolutePath().toString());

        loaded = true;
    }

    private static void extract(String resourcePath, Path target) {
        try (InputStream in = ZXingJnaLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Native resource not found: " + resourcePath);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            // Sur *nix, assure executability/lecture
            try {
                Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (UnsupportedOperationException ignore) { /* Windows */ }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract " + resourcePath + " -> " + target, e);
        }
    }

    private static String detectOs() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("mac") || osName.contains("darwin")) return "macos";
        if (osName.contains("win")) return "windows";
        if (osName.contains("nux") || osName.contains("linux")) return "linux";
        throw new IllegalStateException("Unsupported OS: " + osName);
    }

    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if (arch.contains("x86_64") || arch.contains("amd64")) return "x86_64";
        throw new IllegalStateException("Unsupported Arch: " + arch);
    }
}
