package feature_mining;

import org.w3c.dom.css.Rect;

public class Rectangle {

    int width;
    int height;
    int x;
    int y;
    int depthGradient;
    int scoreGradient;
    int sampleGradient;

    public Rectangle(int width, int height, int x, int y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setDepthGradient(int depthGradient) {
        this.depthGradient = depthGradient;
    }

    public void setScoreGradient(int scoreGradient) {
        this.scoreGradient = scoreGradient;
    }

    public void setSampleGradient(int sampleGradient) {
        this.sampleGradient = sampleGradient;
    }

    public int getDepthGradient() {
        return this.depthGradient;
    }

    public int getScoreGradient() {
        return this.scoreGradient;
    }

    public int getSampleGradient() {
        return this.sampleGradient;
    }
}
