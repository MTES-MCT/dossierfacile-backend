#include <cstdlib>
#include <string>
#include <sstream>
#include <vector>
#include <algorithm> // min/max
#include <cmath>     // floor, ceil, lround

#if __has_include(<ZXing/ReadBarcodes.h>)
  #include <ZXing/ReadBarcodes.h>
  #define ZX_HAS_READBARCODES 1
#else
  #include <ZXing/ReadBarcode.h>
  #define ZX_HAS_READBARCODES 0
#endif

#if __has_include(<ZXing/Barcode.h>)
  #include <ZXing/Barcode.h>
  using ZXResult = ZXing::Barcode;
#else
  #include <ZXing/Result.h>
  using ZXResult = ZXing::Result;
#endif

#if __has_include(<ZXing/ReaderOptions.h>)
  #include <ZXing/ReaderOptions.h>
  using ZXOptions = ZXing::ReaderOptions;
#else
  #include <ZXing/DecodeHints.h>
  using ZXOptions = ZXing::DecodeHints;
#endif

#include <ZXing/BarcodeFormat.h>
#include <ZXing/ImageView.h>
#include <ZXing/TextUtfEncoding.h>

static inline std::string to_utf8(const std::string& s) { return s; }
static inline std::string to_utf8(const std::wstring& ws) { return ZXing::TextUtfEncoding::ToUtf8(ws); }

// JSON escape minimal
static inline std::string json_escape(const std::string& s) {
    std::string out; out.reserve(s.size()+8);
    for (unsigned char c : s) {
        switch (c) {
            case '\\': out += "\\\\"; break;
            case '"':  out += "\\\""; break;
            case '\b': out += "\\b";  break;
            case '\f': out += "\\f";  break;
            case '\n': out += "\\n";  break;
            case '\r': out += "\\r";  break;
            case '\t': out += "\\t";  break;
            default:
                if (c < 0x20) { char buf[7]; std::snprintf(buf, sizeof(buf), "\\u%04x", c); out += buf; }
                else out += c;
        }
    }
    return out;
}

extern "C" const char* zxingcpp_read_lum8(
    const unsigned char* data, int width, int height, int stride, int /*rotateDegrees*/
){
    using namespace ZXing;

    auto run = [&](const ImageView& iv, ReaderOptions opt)->std::vector<ZXing::Barcode> {
        std::vector<ZXing::Barcode> out;

        // Essai #1 : LocalAverage + TryInvert
        opt.setBinarizer(Binarizer::LocalAverage);
        opt.setTryInvert(true);
        out = ReadBarcodes(iv, opt);
        if (!out.empty()) return out;

        // Essai #2 : LocalAverage sans invert
        opt.setTryInvert(false);
        out = ReadBarcodes(iv, opt);
        if (!out.empty()) return out;

        // Essai #3 : GlobalHistogram + TryInvert
        opt.setBinarizer(Binarizer::GlobalHistogram);
        opt.setTryInvert(true);
        out = ReadBarcodes(iv, opt);
        if (!out.empty()) return out;

        // Essai #4 : GlobalHistogram sans invert
        opt.setTryInvert(false);
        out = ReadBarcodes(iv, opt);
        return out;
    };

    // Image d’entrée
    ImageView iv(data, width, height, ImageFormat::Lum, stride);

    // Options de base
    ReaderOptions opt;
    opt.setFormats(BarcodeFormat::QRCode | BarcodeFormat::DataMatrix | BarcodeFormat::PDF417);
    opt.setTryHarder(true);
    opt.setTryRotate(true);
    opt.setIsPure(false);
    opt.setMaxNumberOfSymbols(128);

    // Tente sur l’image d’origine
    auto results = run(iv, opt);

    // Si rien trouvé, on upscale x2 en nearest-neighbor (les Datamatrix minuscules y gagnent beaucoup)
    std::vector<uint8_t> up; int upW = 0, upH = 0, upStride = 0;
    if (results.empty()) {
        upW = width * 2; upH = height * 2; upStride = upW;
        up.resize(static_cast<size_t>(upStride) * upH);
        for (int y = 0; y < height; ++y) {
            const uint8_t* src = data + y * stride;
            uint8_t* dst1 = up.data() + (y * 2) * upStride;
            uint8_t* dst2 = dst1 + upStride;
            for (int x = 0; x < width; ++x) {
                uint8_t v = src[x];
                int dx = x * 2;
                dst1[dx] = v; dst1[dx + 1] = v;
                dst2[dx] = v; dst2[dx + 1] = v;
            }
        }
        ImageView iv2(up.data(), upW, upH, ImageFormat::Lum, upStride);
        results = run(iv2, opt);
    }

    // Construction JSON (points + bbox entiers)
    auto emit_point_int = [](std::ostringstream& os, const char* name, auto p){
        os << "\"" << name << "\":{\"x\":" << static_cast<long>(std::lround(p.x))
           << ",\"y\":" << static_cast<long>(std::lround(p.y)) << "}";
    };

    std::ostringstream os;
    os << "[";
    for (size_t i = 0; i < results.size(); ++i) {
        const auto& r = results[i];
        if (i) os << ",";

        const auto pos = r.position();
        const auto tl = pos.topLeft();
        const auto tr = pos.topRight();
        const auto br = pos.bottomRight();
        const auto bl = pos.bottomLeft();

        // bbox englobante entière
        const double minXf = std::min({(double)tl.x, (double)tr.x, (double)br.x, (double)bl.x});
        const double maxXf = std::max({(double)tl.x, (double)tr.x, (double)br.x, (double)bl.x});
        const double minYf = std::min({(double)tl.y, (double)tr.y, (double)br.y, (double)bl.y});
        const double maxYf = std::max({(double)tl.y, (double)tr.y, (double)br.y, (double)bl.y});

        const int minXi = (int)std::floor(minXf);
        const int maxXi = (int)std::ceil (maxXf);
        const int minYi = (int)std::floor(minYf);
        const int maxYi = (int)std::ceil (maxYf);
        const int w = std::max(0, maxXi - minXi);
        const int h = std::max(0, maxYi - minYi);

        os << "{"
           << "\"format\":\"" << ToString(r.format()) << "\","
           << "\"text\":\""   << json_escape(to_utf8(r.text())) << "\","
           << "\"bbox\":{";
        emit_point_int(os, "topLeft", tl);     os << ",";
        emit_point_int(os, "topRight", tr);    os << ",";
        emit_point_int(os, "bottomLeft", bl);  os << ",";
        emit_point_int(os, "bottomRight", br); os << ",";
        os << "\"width\":"  << w << ",\"height\":" << h << "}"
           << "}";
    }
    os << "]";
    return strdup(os.str().c_str());
}

extern "C" void zxingcpp_free_str(const char* p){
    if (p) free((void*)p);
}
