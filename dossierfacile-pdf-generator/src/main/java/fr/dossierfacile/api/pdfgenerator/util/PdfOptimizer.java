package fr.dossierfacile.api.pdfgenerator.util;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

/**
 * Allow to reduce PDF size by identifying and replacing duplicates COSObjects.
 * Use only if necessary.
 */
public class PdfOptimizer {

    private static String hashCode(COSBase base) {
        if (base instanceof COSArray) {
            // only concat object reference of array
            String hash = Stream.of((COSArray) base)
                    .map(i -> String.valueOf(i.hashCode()))
                    .reduce("array-", (result, item) -> result + "-" + item);
            return hash;
        }
        return base.getClass().getName() + base.hashCode();
    }

    private COSBase getAndReplaceObjectByReferenceObject(COSBase base, List<COSBase> objs, Map<String, COSBase> references) throws IOException {
        if (base == null || objs.contains(base)) {
            // empty or object already parsed
            return base;
        } else {
            objs.add(base);

            if (base instanceof COSDictionary dico) {
                StringBuilder hash = new StringBuilder("d-");
                for (Map.Entry<COSName, COSBase> x : ((COSDictionary) base).entrySet()) {
                    COSBase o = getAndReplaceObjectByReferenceObject(x.getValue(), objs, references);
                    dico.setItem(x.getKey(), o);
                    hash.append(x.getKey().toString() + hashCode(o));
                }

                if (base instanceof COSStream) {
                    InputStream stream = ((COSStream) base).createRawInputStream();
                    byte[] b = IOUtils.toByteArray(stream);
                    hash.append(Arrays.hashCode(b));
                    stream.close();
                }

                String key = hash.toString();
                if (references.get(key) == null) {
                    references.put(key, base);
                }
                return references.get(key);
            } else if (base instanceof COSArray) {

                for (int i = 0; i < ((COSArray) base).size(); i++) {
                    COSBase x = ((COSArray) base).get(i);
                    ((COSArray) base).set(i, getAndReplaceObjectByReferenceObject(x, objs, references));
                }

                String key = hashCode(base);
                if (references.get(key) == null) {
                    references.put(key, base);
                }
                return references.get(key);
            } else {
                if (base instanceof COSObject obj) {
                    // By pass useless object
                    return getAndReplaceObjectByReferenceObject(obj.getObject(), objs, references);
                } else {
                    // replace by reference if exists - add if not
                    String key = hashCode(base);
                    if (references.get(key) == null) {
                        references.put(key, base);
                    }
                    return base;
                }
            }
        }
    }

    /**
     * reduce the PDDocument size
     */
    public void optimize(PDDocument pdDocument) throws IOException {
        COSDictionary catalogDictionary = pdDocument.getDocumentCatalog().getCOSObject();
        getAndReplaceObjectByReferenceObject(catalogDictionary, new LinkedList<>(), new HashMap<>());
    }

}
