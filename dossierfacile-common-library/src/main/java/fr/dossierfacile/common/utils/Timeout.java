package fr.dossierfacile.common.utils;

import java.util.concurrent.TimeUnit;

public record Timeout(long value, TimeUnit unit) {
}
