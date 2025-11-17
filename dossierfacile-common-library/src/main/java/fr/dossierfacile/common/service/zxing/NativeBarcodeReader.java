package fr.dossierfacile.common.service.zxing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Pointer;
import fr.dossierfacile.common.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class NativeBarcodeReader {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<BarcodeHit> decode(BufferedImage img) {
        ImageUtils.GrayBytes gb = ImageUtils.toGrayBytes(img);
        Pointer p = ZXingNative.INSTANCE.zxingcpp_read_image(gb.data, gb.width, gb.height, 1, gb.stride);
        if (p == null) return List.of();
        String json = p.getString(0);
        ZXingNative.INSTANCE.zxingcpp_free_str(p);

        try {
            List<BarcodeHit> hits = mapper.readValue(json, new TypeReference<>() {
            });
            LinkedHashMap<String, BarcodeHit> uniq = new LinkedHashMap<>();
            for (BarcodeHit h : hits) uniq.put(h.format() + "|" + h.text(), h);
            return new ArrayList<>(uniq.values());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse native JSON: " + json, e);
        }
    }
}

