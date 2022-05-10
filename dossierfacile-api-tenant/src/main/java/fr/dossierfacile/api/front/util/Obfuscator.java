package fr.dossierfacile.api.front.util;

public class Obfuscator {
    public static String email(String email) {
        if (email == null) return null;
        return email.replaceAll("(?<=.{3}).(?=.*@)", "*");
    }
}
