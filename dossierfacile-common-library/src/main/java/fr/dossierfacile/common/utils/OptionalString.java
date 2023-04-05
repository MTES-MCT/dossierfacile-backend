package fr.dossierfacile.common.utils;

import org.apache.commons.lang3.StringUtils;

public class OptionalString {
    private final String str;

    private OptionalString(String str) {
        this.str = str;
    }

    public static OptionalString of(final String in) {
        return new OptionalString(in);
    }

    /**
     * @return elseStr if optional string is Blank.
     */
    public String orElse(final String elseStr) {
        if (StringUtils.isBlank(str))
            return elseStr;
        return str;
    }

    public String get() {
        return str;
    }
}
