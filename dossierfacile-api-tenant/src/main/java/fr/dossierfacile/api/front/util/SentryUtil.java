package fr.dossierfacile.api.front.util;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

public class SentryUtil {
    public static String captureMessage(String msg, SentryLevel level) {
        return msg + "(SentryId:" + Sentry.captureMessage(msg, level) + ")";
    }

    public static String captureMessage(String msg) {
        return captureMessage(msg, SentryLevel.INFO);
    }
}
