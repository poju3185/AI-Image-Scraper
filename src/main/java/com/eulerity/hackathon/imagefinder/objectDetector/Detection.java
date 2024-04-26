package com.eulerity.hackathon.imagefinder.objectDetector;

import lombok.Getter;
import org.opencv.core.Rect2d;

@Getter
public class Detection {
    private final Rect2d bbox;
    private final float score;
    private final String className;

    public Detection(Rect2d bbox, float score, String className) {
        this.bbox = bbox;
        this.score = score;
        this.className = className;
    }

    @Override
    public String toString() {
        return "Class ID: " + className + ", Score: " + score + ", BBox: " + bbox;
    }
}

