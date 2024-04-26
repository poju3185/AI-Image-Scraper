package com.eulerity.hackathon.imagefinder.ocr;

import com.google.gson.Gson;
import lombok.Data;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * This class provides functionality to interact with the OCR.space API for optical character recognition.
 */
public class OCRApi {
    private static final Properties config = new Properties();
    private static final String OCR_URL = "https://api.ocr.space/parse/image";
    private static final Gson gson = new Gson();

    static {
        // Load the properties file
        try (InputStream input = OCRApi.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading the properties file", e);
        }
    }

    /**
     * Performs OCR on an image located at a given URL based on its MIME type.
     *
     * @param imageUrl The URL of the image to be processed.
     * @return The text detected from the image or an empty string if an error occurs.
     * @throws IOException         If there is an I/O error during image fetching or MIME type determination.
     * @throws TranscoderException If there is an error in converting SVG images.
     */
    public static String performOCROnImageUrl(String imageUrl) throws IOException, TranscoderException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("apikey", config.getProperty("OCR_API_KEY")); // API key for the OCR service
        parameters.put("url", imageUrl); // URL of the image

        String mimeType = getMimeType(imageUrl); // Determine the MIME type of the image

        try {
            switch (mimeType) {
                case "image/svg+xml":
                    return performOCROnSvg(imageUrl, true);
                case "image/png":
                    String result = sendPostRequest(parameters);
                    if (!result.isEmpty()) return result;
                    URL url = new URL(imageUrl);
                    BufferedImage image = ImageIO.read(url);
                    return performOCROnPng(image);
                case "image/x-icon":
                    return "";
                default:
                    return sendPostRequest(parameters);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing OCR for the image at URL: " + imageUrl);
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This method supports both SVG content provided directly as a string and SVG content accessible via a URL.
     *
     * @param svgString The SVG content or URL to the SVG.
     * @param isUrl     Indicates whether the svgString parameter is a URL.
     * @return The text detected from the OCR process, or an empty string if no text is detected.
     * @throws IOException         If an I/O error occurs during image handling.
     * @throws TranscoderException If an error occurs during SVG to PNG transcoding.
     */
    public static String performOCROnSvg(String svgString, boolean isUrl) throws IOException, TranscoderException {
        TranscoderInput input;
        if (isUrl) {
            input = new TranscoderInput(svgString);
        } else {
            input = new TranscoderInput(new StringReader(svgString));
        }

        // Prepare a ByteArrayOutputStream to catch the transcoder's output
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(imageOutputStream);

        // Use the PNGTranscoder from Batik to convert SVG to PNG
        PNGTranscoder pngTranscoder = new PNGTranscoder();
        pngTranscoder.transcode(input, output);

        // Extract the byte array of the transcoded PNG
        byte[] imageBytes = imageOutputStream.toByteArray();
        imageOutputStream.close();

        // Convert byte array into a BufferedImage for OCR processing
        ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage bufferedImage = ImageIO.read(imageInputStream);

        return performOCROnPng(bufferedImage);
    }

    /**
     * Performs OCR on a PNG image with consideration for images with transparency.
     * This method first tries to perform OCR with a white background. If no text is detected
     * and the image has an alpha channel (transparency), it retries OCR with a black background.
     * This approach helps in handling images where text might not be visible against certain backgrounds
     * due to transparency.
     *
     * @param image The BufferedImage on which OCR is to be performed.
     * @return The detected text from the OCR process, or an empty string if no text is detected.
     * @throws IOException if there is an error in the OCR process.
     */
    public static String performOCROnPng(BufferedImage image) throws IOException {
        String ocrDetectedText = changeBgAndPerformOCR(image, Color.WHITE);
        if (ocrDetectedText.isEmpty() && image.getColorModel().hasAlpha()) {
            return changeBgAndPerformOCR(image, Color.BLACK);
        }
        return ocrDetectedText;
    }

    /**
     * Fetches the MIME type of the content at the specified URL.
     *
     * @param url The URL of the content for which the MIME type is to be determined.
     * @return The MIME type of the content, or an empty string if an error occurs or the MIME type is not available.
     */
    public static String getMimeType(String url) {
        String result = "";
        try {
            Response response = Request.get(url).execute();
            result = response.returnContent().getType().getMimeType();
        } catch (IOException e) {
            System.out.println(url);
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Changes the background color of the provided image and performs OCR on it.
     *
     * @param image           The original BufferedImage on which background color change is performed.
     * @param backgroundColor The new background color to be set.
     * @return The OCR extracted text or an empty string if an error occurs.
     */
    private static String changeBgAndPerformOCR(BufferedImage image, Color backgroundColor) {
        // Create a new BufferedImage with the same dimensions as the original but with specified background color.
        BufferedImage background = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = background.createGraphics();

        // Set the new background color.
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, background.getWidth(), background.getHeight());  // Fill the entire area with the background color.
        g2d.drawImage(image, 0, 0, null);  // Draw the original image over the colored background.
        g2d.dispose();  // Dispose of the graphics context to free up resources.

        image = background;

        String encodedImage;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            // Encode the modified image to PNG and then to a base64 string.
            ImageIO.write(image, "PNG", bos);
            byte[] imageBytes = bos.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            encodedImage = encoder.encodeToString(imageBytes);
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        // Prepare parameters for the OCR API request.
        Map<String, String> parameters = new HashMap<>();
        parameters.put("apikey", config.getProperty("OCR_API_KEY"));
        parameters.put("base64Image", "data:image/png;base64," + encodedImage);
        return sendPostRequest(parameters);
    }

    /**
     * Sends a POST request to the COR URL with the given parameters.
     *
     * @param postDataParams A map containing the POST data parameters.
     * @return The parsed text from the OCR response if successful, otherwise an error message.
     */
    private static String sendPostRequest(Map<String, String> postDataParams) {
        String result = null;
        Request request = Request.post(OCRApi.OCR_URL);
        List<NameValuePair> paramsList = new ArrayList<>();
        // Prepare the POST body parameters
        for (Map.Entry<String, String> entry : postDataParams.entrySet()) {
            paramsList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        request.bodyForm(paramsList);

        try {
            // Execute the POST request and get the response as a string
            result = request.execute().returnContent().asString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse the JSON response using Gson
        OCRApiResponse response = gson.fromJson(result, OCRApiResponse.class);
        if (!response.isIsErroredOnProcessing()) {
            // Since we are not sending a multi-page PDF, we only process the first result.
            return response.getParsedResults().get(0).getParsedText();
        } else {
            System.out.println("Errored when processing ocr: " + result);
            return "";
        }
    }

    public static void main(String[] args) {
        try {
            String urlResult = performOCROnImageUrl("https://www.goodhousekeeping.com/_assets/design-tokens/goodhousekeeping/static/images/logos/logo.dc34ecc.svg?primary=navLogoColor");
            System.out.println("OCR Result for URL: " + urlResult);
        } catch (IOException | TranscoderException e) {
            e.printStackTrace();
        }
    }

}

@Data
class OCRApiResponse {
    private List<ParsedResult> ParsedResults;
    private int OCRExitCode;
    private boolean IsErroredOnProcessing;
    private String ProcessingTimeInMilliseconds;
    private String SearchablePDFURL;
}

@Data
class ParsedResult {
    private TextOverlay TextOverlay;
    private String TextOrientation;
    private int FileParseExitCode;
    private String ParsedText;
    private String ErrorMessage;
    private String ErrorDetails;
}


@Data
class TextOverlay {
    private List<Object> Lines;
    private boolean HasOverlay;
    private String Message;
}