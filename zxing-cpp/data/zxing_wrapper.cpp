#include <cstdlib>
#include <string>
#include <sstream>
#include <vector>
#include <cmath>
#include <cstring>

// On utilise directement les headers de la librairie
#include "ReadBarcode.h"
#include "ReaderOptions.h"
#include "ImageView.h"
#include "BarcodeFormat.h"
#include "TextUtfEncoding.h"

// Helper JSON (Gardé tel quel car il est très bien)
static inline std::string json_escape(const std::string& s) {
    std::string out; out.reserve(s.size() + 8);
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

extern "C" {

    // Fonction principale à appeler depuis JNA
    // data : pointeur vers les pixels
    // width/height : dimensions
    // channels : 1 (Lum/Gris), 3 (RGB/BGR), 4 (RGBA)
    // rowStride : nombre d'octets par ligne (souvent width * channels)
    const char* zxingcpp_read_image(const unsigned char* data, int width, int height, int channels, int rowStride) {

        using namespace ZXing;

        // 1. Détermination du format
        ImageFormat fmt = ImageFormat::None;
        switch (channels) {
            case 1: fmt = ImageFormat::Lum; break;
            case 3: fmt = ImageFormat::RGB; break; // Note: ZXing gère RGB ou BGR de façon similaire pour la luminance
            case 4: fmt = ImageFormat::RGBX; break;
            default: fmt = ImageFormat::Lum; break;
        }

        try {
            // 2. Création de la vue (Zero Copy)
            ImageView image(data, width, height, fmt, rowStride);

            // 3. Configuration C++20 (La "Winning Config" de nos tests)
            ReaderOptions options;

            // Formats ciblés
            options.setFormats(BarcodeFormat::DataMatrix | BarcodeFormat::QRCode | BarcodeFormat::PDF417);

            // Algorithmes robustes
            options.setTryHarder(true);       // Analyse approfondie
            options.setTryRotate(true);       // Supporte rotations 90/180/270

            // LA CLE DU SUCCES SUR DEBIAN 12 :
            options.setTryDownscale(true);    // Permet de scanner la page entière (A4)

            // 4. Lecture
            auto results = ReadBarcodes(image, options);

            // 5. Construction du JSON
            std::ostringstream os;
            os << "[";
            for (size_t i = 0; i < results.size(); ++i) {
                const auto& r = results[i];
                if (i > 0) os << ",";

                auto pos = r.position();

                // Récupération des 4 points du polygone
                auto tl = pos.topLeft();
                auto tr = pos.topRight();
                auto bl = pos.bottomLeft();
                auto br = pos.bottomRight();

                // Calcul bbox englobante (width/height) pour ton record
                int minX = std::min({tl.x, tr.x, bl.x, br.x});
                int minY = std::min({tl.y, tr.y, bl.y, br.y});
                int maxX = std::max({tl.x, tr.x, bl.x, br.x});
                int maxY = std::max({tl.y, tr.y, bl.y, br.y});
                int w = maxX - minX;
                int h = maxY - minY;

                // Construction du JSON correspondant à tes Records Java
                os << "{"
                   << "\"format\":\"" << ToString(r.format()) << "\","
                   << "\"text\":\""   << json_escape(r.text()) << "\","
                   << "\"bbox\":{"
                       << "\"topLeft\":{\"x\":" << tl.x << ",\"y\":" << tl.y << "},"
                       << "\"topRight\":{\"x\":" << tr.x << ",\"y\":" << tr.y << "},"
                       << "\"bottomLeft\":{\"x\":" << bl.x << ",\"y\":" << bl.y << "},"
                       << "\"bottomRight\":{\"x\":" << br.x << ",\"y\":" << br.y << "},"
                       << "\"width\":" << w << ","
                       << "\"height\":" << h
                   << "}"
                   << "}";
            }
            os << "]";

            // On retourne une copie de la string
            return strdup(os.str().c_str());

        } catch (...) {
            return strdup("[]"); // En cas d'erreur grave, tableau vide
        }
    }

    // Helper pour libérer la mémoire de la string retournée depuis Java
    void zxingcpp_free_str(char* p) {
        if (p) free(p);
    }
}