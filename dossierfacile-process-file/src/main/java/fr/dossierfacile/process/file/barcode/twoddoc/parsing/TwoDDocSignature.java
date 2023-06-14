package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import org.apache.commons.codec.binary.Base32;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public record TwoDDocSignature(byte[] signature) {

    static TwoDDocSignature decodeBase32(String encoded) {
        byte[] decoded = new Base32().decode(encoded.getBytes());
        return new TwoDDocSignature(decoded);
    }

    public byte[] encodeDer() {
        try (var outputStream = new ByteArrayOutputStream()) {
            var sequenceGenerator = new DERSequenceGenerator(outputStream);
            addASN1IntegerTo(sequenceGenerator, 0);
            addASN1IntegerTo(sequenceGenerator, signature.length / 2);
            sequenceGenerator.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error while encoding raw signature to DER ASN.1", e);
        }
    }

    private void addASN1IntegerTo(DERSequenceGenerator sequenceGenerator, int startingPositionInSignature) throws IOException {
        var halfLength = signature.length / 2;
        var byteArray = new byte[halfLength + 1];
        System.arraycopy(signature, startingPositionInSignature, byteArray, 1, halfLength);
        sequenceGenerator.addObject(new ASN1Integer(new BigInteger(byteArray).toByteArray()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TwoDDocSignature that = (TwoDDocSignature) o;
        return Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signature);
    }

    @Override
    public String toString() {
        return "TwoDDocSignature{" +
                "signature=" + Arrays.toString(signature) +
                '}';
    }

}
