package com.eulerity.hackathon.imagefinder.objectDetector;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ref: https://blog.csdn.net/taoli188/article/details/134720614

/**
 * This class is a singleton that provides an interface to perform object detection using a neural network model.
 */
public class ObjectDetector {
    private static ObjectDetector instance;
    private final Net net;
    private Map<Integer, String> classIdToNameMap;

    private ObjectDetector() {
        this.net = Dnn.readNetFromONNX("yolov8n.onnx");
        initializeClassIdToNameMap();
    }

    public static synchronized ObjectDetector getInstance() {
        if (instance == null) {
            instance = new ObjectDetector();
        }
        return instance;
    }

    /**
     * Predicts the objects in an image retrieved from a specified URL.
     *
     * @param imageUrl The URL of the image to be analyzed.
     * @return A list of Detection objects.
     */
    public List<Detection> detect(URL imageUrl) {
        Mat mat = ImageAnnotator.urlToImage(imageUrl);

        if (mat.empty()) {
            System.out.println("Failed to decode image from URL: " + imageUrl);
            return new ArrayList<>();
        }

        return processImage(mat);
    }

    /**
     * Detects the objects in an image retrieved from a local file.
     *
     * @param imagePath the path to the image file to be analyzed.
     * @return a list of Detection objects.
     */
    public List<Detection> detect(Path imagePath) {
        Mat mat = Imgcodecs.imread(imagePath.toString());
        if (mat.empty()) {
            System.out.println("Failed to load image from path: " + imagePath);
            return new ArrayList<>();
        }

        return processImage(mat);
    }

    /**
     * Processes an image using a deep learning model to generate detections.
     *
     * @param image the image to be processed, encapsulated in a Mat object.
     * @return a list of Detection objects.
     */
    private List<Detection> processImage(Mat image) {
        // Preprocess the image to create a blob that can be fed into the neural network.
        Mat blob = Dnn.blobFromImage(image, 1 / 255.0, new Size(640, 640), new Scalar(0), true, false);
        net.setInput(blob);
        Mat predict = net.forward();
        return processPredictions(predict, image);
    }


    /**
     * Processes the predictions obtained from the deep learning model and converts the results into a list of Prediction objects.
     *
     * @param predict the raw prediction data from the neural network.
     * @param mat     the original image Mat used for scaling bounding box dimensions.
     * @return a list of Detection objects.
     */
    private List<Detection> processPredictions(Mat predict, Mat mat) {
        Mat mask = predict.reshape(0, 1).reshape(0, predict.size(1));
        // Calculate scaling factors to map detections back to the original image size
        double width = mat.cols() / 640.0;
        double height = mat.rows() / 640.0;

        // Prepare arrays to store the decoded detections
        Rect2d[] rect2d = new Rect2d[mask.cols()];
        float[] scoref = new float[mask.cols()];
        int[] classId = new int[mask.cols()];

        // Decode bounding boxes and class IDs from the prediction mask
        for (int i = 0; i < mask.cols(); i++) {
            double[] x = mask.col(i).get(0, 0);
            double[] y = mask.col(i).get(1, 0);
            double[] w = mask.col(i).get(2, 0);
            double[] h = mask.col(i).get(3, 0);
            rect2d[i] = new Rect2d((x[0] - w[0] / 2) * width, (y[0] - h[0] / 2) * height, w[0] * width, h[0] * height);

            // Extract scores and find the maximum score indicating the most likely class
            Mat score = mask.col(i).submat(4, predict.size(1) - 1, 0, 1);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(score);
            scoref[i] = (float) mmr.maxVal;
            classId[i] = (int) mmr.maxLoc.y;
        }

        // Apply non-maximum suppression to reduce overlapping bounding boxes
        MatOfRect2d bboxes = new MatOfRect2d(rect2d);
        MatOfFloat scores = new MatOfFloat(scoref);
        MatOfInt indices = new MatOfInt();
        Dnn.NMSBoxes(bboxes, scores, 0.3f, 0.5f, indices);

        // Construct Detection objects for each filtered index
        List<Detection> detections = new ArrayList<>();
        if (!indices.empty()) {
            for (int index : indices.toArray()) {
                // Add the new Detection object, converting class ID to a readable name using a mapping
                detections.add(new Detection(rect2d[index], scoref[index], classIdToNameMap.get(classId[index])));
            }
        }

        return detections;
    }

    private void initializeClassIdToNameMap() {
        classIdToNameMap = new HashMap<>();
        classIdToNameMap.put(0, "person");
        classIdToNameMap.put(1, "bicycle");
        classIdToNameMap.put(2, "car");
        classIdToNameMap.put(3, "motorcycle");
        classIdToNameMap.put(4, "airplane");
        classIdToNameMap.put(5, "bus");
        classIdToNameMap.put(6, "train");
        classIdToNameMap.put(7, "truck");
        classIdToNameMap.put(8, "boat");
        classIdToNameMap.put(9, "traffic light");
        classIdToNameMap.put(10, "fire hydrant");
        classIdToNameMap.put(11, "stop sign");
        classIdToNameMap.put(12, "parking meter");
        classIdToNameMap.put(13, "bench");
        classIdToNameMap.put(14, "bird");
        classIdToNameMap.put(15, "cat");
        classIdToNameMap.put(16, "dog");
        classIdToNameMap.put(17, "horse");
        classIdToNameMap.put(18, "sheep");
        classIdToNameMap.put(19, "cow");
        classIdToNameMap.put(20, "elephant");
        classIdToNameMap.put(21, "bear");
        classIdToNameMap.put(22, "zebra");
        classIdToNameMap.put(23, "giraffe");
        classIdToNameMap.put(24, "backpack");
        classIdToNameMap.put(25, "umbrella");
        classIdToNameMap.put(26, "handbag");
        classIdToNameMap.put(27, "tie");
        classIdToNameMap.put(28, "suitcase");
        classIdToNameMap.put(29, "frisbee");
        classIdToNameMap.put(30, "skis");
        classIdToNameMap.put(31, "snowboard");
        classIdToNameMap.put(32, "sports ball");
        classIdToNameMap.put(33, "kite");
        classIdToNameMap.put(34, "baseball bat");
        classIdToNameMap.put(35, "baseball glove");
        classIdToNameMap.put(36, "skateboard");
        classIdToNameMap.put(37, "surfboard");
        classIdToNameMap.put(38, "tennis racket");
        classIdToNameMap.put(39, "bottle");
        classIdToNameMap.put(40, "wine glass");
        classIdToNameMap.put(41, "cup");
        classIdToNameMap.put(42, "fork");
        classIdToNameMap.put(43, "knife");
        classIdToNameMap.put(44, "spoon");
        classIdToNameMap.put(45, "bowl");
        classIdToNameMap.put(46, "banana");
        classIdToNameMap.put(47, "apple");
        classIdToNameMap.put(48, "sandwich");
        classIdToNameMap.put(49, "orange");
        classIdToNameMap.put(50, "broccoli");
        classIdToNameMap.put(51, "carrot");
        classIdToNameMap.put(52, "hot dog");
        classIdToNameMap.put(53, "pizza");
        classIdToNameMap.put(54, "donut");
        classIdToNameMap.put(55, "cake");
        classIdToNameMap.put(56, "chair");
        classIdToNameMap.put(57, "couch");
        classIdToNameMap.put(58, "potted plant");
        classIdToNameMap.put(59, "bed");
        classIdToNameMap.put(60, "dining table");
        classIdToNameMap.put(61, "toilet");
        classIdToNameMap.put(62, "tv");
        classIdToNameMap.put(63, "laptop");
        classIdToNameMap.put(64, "mouse");
        classIdToNameMap.put(65, "remote");
        classIdToNameMap.put(66, "keyboard");
        classIdToNameMap.put(67, "cell phone");
        classIdToNameMap.put(68, "microwave");
        classIdToNameMap.put(69, "oven");
        classIdToNameMap.put(70, "toaster");
        classIdToNameMap.put(71, "sink");
        classIdToNameMap.put(72, "refrigerator");
        classIdToNameMap.put(73, "book");
        classIdToNameMap.put(74, "clock");
        classIdToNameMap.put(75, "vase");
        classIdToNameMap.put(76, "scissors");
        classIdToNameMap.put(77, "teddy bear");
        classIdToNameMap.put(78, "hair drier");
        classIdToNameMap.put(79, "toothbrush");
    }

    public static void main(String[] args) throws MalformedURLException {
        nu.pattern.OpenCV.loadShared();
        List<Detection> detections = ObjectDetector.getInstance().detect(new URL("https://i.guim.co.uk/img/media/fea5eccc6183240a3f6e910de9541dd24fb1f7d2/141_395_3671_2202/master/3671.jpg?width=445&dpr=1&s=none"));
        for (Detection detection : detections) {
            System.out.println(detection);
        }
    }
}


