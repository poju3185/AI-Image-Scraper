package com.eulerity.hackathon.imagefinder.objectDetector;

import com.eulerity.hackathon.imagefinder.objectDetector.Detection;
import lombok.Getter;

import java.util.List;

@Getter
public class AnnotateRequest {
    private final String url;
    private final List<Detection> detections;

    public AnnotateRequest(String url, List<Detection> detections) {
        this.url = url;
        this.detections = detections;
    }

    @Override
    public String toString() {
        return "AnnotateRequest{" +
                "url='" + url + '\'' +
                ", detections=" + detections +
                '}';
    }
}
