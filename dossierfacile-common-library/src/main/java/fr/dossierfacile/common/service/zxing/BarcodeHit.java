package fr.dossierfacile.common.service.zxing;

public record BarcodeHit(String format, String text, BarcodeBbox bbox) {}
