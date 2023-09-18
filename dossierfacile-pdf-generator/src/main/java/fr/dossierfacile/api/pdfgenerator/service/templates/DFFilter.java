package fr.dossierfacile.api.pdfgenerator.service.templates;

import com.jhlabs.image.TransformFilter;

import java.awt.image.BufferedImage;

/**
 * A filter which simulates a lens placed over an image.
 */
public class DFFilter extends TransformFilter {
    private float width;
    private float height;
    private int xFrequency = 12;
    private int yFrequency = 8;
    private int maxDistorsion = 28;

    public DFFilter() {
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        this.width = src.getWidth();
        this.height = src.getHeight();
        return super.filter(src, dst);
    }

    protected void transformInverse(int x, int y, float[] out) {
        out[0] = x;
        float r = (float) Math.sin(x * xFrequency  / width);
        out[1] = y + maxDistorsion * (float) Math.sin(y * yFrequency / height) * r * r;
    }

    public String toString() {
        return "Distort...";
    }

    public int getxFrequency() {
        return xFrequency;
    }

    public void setxFrequency(int xFrequency) {
        this.xFrequency = xFrequency;
    }

    public int getyFrequency() {
        return yFrequency;
    }

    public void setyFrequency(int yFrequency) {
        this.yFrequency = yFrequency;
    }

    public int getMaxDistorsion() {
        return maxDistorsion;
    }

    public void setMaxDistorsion(int maxDistorsion) {
        this.maxDistorsion = maxDistorsion;
    }
}
