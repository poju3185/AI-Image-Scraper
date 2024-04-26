package com.eulerity.hackathon.imagefinder.crawler;

import com.eulerity.hackathon.imagefinder.objectDetector.Detection;
import com.eulerity.hackathon.imagefinder.objectDetector.ObjectDetector;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class CrawlerImgResult extends CrawlerResult {
    private final String imgUrl;
    private Set<String> classes;
    private List<Detection> detections;

    public CrawlerImgResult(String imgUrl) {
        this.resultType = ResultType.IMAGE_RESULT;
        this.imgUrl = imgUrl;
    }
    @Override
    public boolean canRunAsynchronously() {
        return false;
    }

    @Override
    public void useAI() {
        useObjectDetection();
    }

    private void useObjectDetection() {
        ObjectDetector objectDetector = ObjectDetector.getInstance();
        URL url;
        try {
            url = new URL(imgUrl);
        } catch (MalformedURLException e) {
            return;
        }
        detections = objectDetector.detect(url);
        classes = new HashSet<>();
        for (Detection detection : detections) {
            classes.add(detection.getClassName());
        }
    }

    @Override
    public String toString() {
        return "CrawlerImgResult{" +
                "imgUrl='" + imgUrl + '\'' +
                ", classes=" + classes +
                ", detections=" + detections +
                '}';
    }
}
