package fr.dossierfacile.common.service.zxing;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface ZXingNative extends Library {
    ZXingNative INSTANCE = Native.load("zxing_jna", ZXingNative.class);

    // channels: 1 pour gris, 3 pour RGB/BGR
    Pointer zxingcpp_read_image(byte[] data, int width, int height, int channels, int stride);
    void zxingcpp_free_str(Pointer p);
}
