package com.eulerity.hackathon.imagefinder.objectDetector;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * This class provides utility functions for annotating images with detection results.
 */
public class ImageAnnotator {

    /**
     * Annotates an image with detections.
     *
     * @param image      The image to annotate as an OpenCV Mat object.
     * @param detections A list of Detection objects.
     * @return An annotated image as an OpenCV Mat object.
     */
    public static Mat annotateImage(Mat image, List<Detection> detections) {
        if (detections == null) {
            return image;
        }

        for (Detection detection : detections) {
            Rect2d bbox = detection.getBbox();  // Bounding box for the detection
            float score = detection.getScore();  // Confidence score of the detection
            String classId = detection.getClassName();  // Readable name of the detected object

            // Draw a rectangle around the detected object.
            Imgproc.rectangle(image, new org.opencv.core.Point(bbox.x, bbox.y), new org.opencv.core.Point(bbox.x + bbox.width, bbox.y + bbox.height), new Scalar(0, 0, 255), 2);

            String label = classId + ": " + String.format("%.2f", score);

            // Put the label on the image.
            Imgproc.putText(image, label, new org.opencv.core.Point(bbox.x, bbox.y), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 1);
        }

        return image;
    }

    /**
     * Converts an image from a URL to an OpenCV Mat object.
     *
     * @param url The URL from which to load the image.
     * @return A Mat object representing the loaded and decoded image. If the image cannot be loaded, returns an empty Mat object.
     */
    public static Mat urlToImage(URL url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            Response response = Request.get(url.toString()).setHeader("User-Agent", "Mozilla/5.0").execute();
            byte[] imageData = response.returnContent().asBytes();
            return Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
//            HttpGet request = new HttpGet(url.toURI());
//            request.setHeader("User-Agent", "Mozilla/5.0");
//            try (CloseableHttpResponse response = client.execute(request)) {
//                byte[] imageData = EntityUtils.toByteArray(response.getEntity());
//                return Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
//            }
        } catch (Exception e) {
            System.out.println("Couldn't load image from url: " + url);
            return new Mat();
        }
    }

    /**
     * Converts an image from a URL string to an OpenCV Mat object.
     *
     * @param urlString The string representation of the URL from which to load the image.
     * @return A Mat object representing the loaded and decoded image. Returns an empty Mat object if errors occur.
     */
    public static Mat urlToImage(String urlString) {
        try {
            // Encode the URL string to handle spaces
            String encodedUrlString = urlString.replace(" ", "%20");

            URL url = new URL(encodedUrlString);

            return urlToImage(url);
        } catch (IOException e) {
            System.err.println("Error encoding or loading image from URL string: " + e.getMessage());
            return new Mat();
        }
    }

    /**
     * Converts an OpenCV Mat object to a byte array representation of the image.
     *
     * @param image The Mat object containing the image data to be encoded.
     * @return A byte array containing the JPEG encoded image data.
     * @throws IOException              If the encoding process fails.
     * @throws IllegalArgumentException If the provided Mat object is null or empty.
     */
    public static byte[] matToBytes(Mat image) throws IOException {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Provided Mat object is empty or null.");
        }

        MatOfByte buffer = new MatOfByte();

        boolean result = Imgcodecs.imencode(".jpg", image, buffer);
        if (!result) {
            throw new IOException("Failed to encode image.");
        }

        return buffer.toArray();
    }
}
