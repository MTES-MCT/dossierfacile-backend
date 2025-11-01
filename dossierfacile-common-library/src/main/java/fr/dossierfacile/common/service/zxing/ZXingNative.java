package fr.dossierfacile.common.service.zxing;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface ZXingNative extends Library {
    ZXingNative INSTANCE = Native.load("zxing_jna", ZXingNative.class);

    Pointer zxingcpp_read_lum8(byte[] data, int width, int height, int stride, int rotateDeg);
    void zxingcpp_free_str(Pointer p);
}
