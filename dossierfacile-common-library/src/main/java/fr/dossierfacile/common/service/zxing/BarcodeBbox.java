package fr.dossierfacile.common.service.zxing;

public record BarcodeBbox(
        BarcodePoint topLeft,
        BarcodePoint topRight,
        BarcodePoint bottomLeft,
        BarcodePoint bottomRight,
        int width,
        int height
) {}

